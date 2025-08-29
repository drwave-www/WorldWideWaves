#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Generate per-language Moko resources from base/strings.xml using OpenAI.

Constraints:
- Only translate new/modified/missing strings (hash stored in target comments).
- One prompt per language.
- Uses the last full-line comment before each <string> plus an optional inline comment.
- Logs created/modified/deleted entries.
- Declares LANGUAGES at top.
- Handles ar/ko/zh with UTF-8 and plain text.
- Adds GLOBAL_CONTEXT and TRANSLATION_CONSIGNS to prompt.
- Respects tone and preserves placeholders.
"""

import argparse
import hashlib
import json
import os
import re
import sys
from datetime import datetime
from pathlib import Path

# ---- Configure here ---------------------------------------------------------

LANGUAGES = ["ru", "fr", "es", "pt", "it", "de", "id", "ko", "tr", "sw", "ja", "zh", "pa", "ar"]
MODEL = os.getenv("OPENAI_MODEL", "gpt-5")       # override via OPENAI_MODEL
GLOBAL_CONTEXT = """
You know this : if the Earth's story were told in a 1,000-page book, humanity would appear only in the last paragraph of the very last page.

But this brief story could end sooner than expected. We are isolated in a vast a desolate universe. We destroy everything. We are selfish. We are cruel. We are greedy. We are blind. And we seek to dominate. But we are also capable of empathy, we create wonders, we are resilient, we can be altruistic.

But we have little time. We have made so much progress.. We can build an alternative : a community of rights, of solidarity, of democracy. It's up to us.

Join a global movement, a peaceful awakening. Let's create human waves, a testimony of our true humanity, a strong command to our leaders. Let's gather around cities and countries, all over the world. Let's choose peace. Let's choose joy. Let's choose nature. Let's choose US.

What if just one wave was enough to bring us together?

WorldWideWaves is an ephemeral mobile app with a visionary goal: to orchestrate a series of human waves that ripple through great cities, across countries. Set to unfold over the span of one year, this initiative aims to transcend physical and cultural boundaries, fostering a sense of unity, community, and shared human experience.

At its core, WorldWideWaves is more than just an app, it's a global movement designed to bring people together in a display of solidarity and joy, with shared values.

By leveraging technology, WorldWideWaves will enable participants from around the world to join in these human waves in their cities, creating a powerful visual and emotional spectacle.

In essence, WorldWideWaves is an ambitious endeavor to harness the power of technology for a truly noble cause, connecting humanity in a celebration of unity, joy and collective action. Through each wave, participants will not only contribute to a global spectacle but also feel and share with the world the profound impact of coming together as one.
"""                  # explain the app’s goal here
TRANSLATION_CONSIGNS = """
Goal: produce natural, concise UI translations that match the source tone and intent.

General rules:
- Do NOT translate proper nouns (app name, neighborhoods, venues, brand names, hashtags, @handles, but translate countries and cities names if it's relevant
- Prefer each language’s standard exonym/endonym for countries and cities when such localization is customary; otherwise keep the English name.
- Keep placeholders and markup exactly as-is: %s, %1$s, %d, {{var}}, <b>…</b>, <i>…</i>, etc.
- Preserve punctuation, numbers, and capitalization patterns unless the target language’s UI convention clearly differs.
- Keep the same register and energy as the source. If the source is an imperative CTA, keep it imperative. Avoid added emojis or extra exclamation marks.
- Respect any length hint in comments (Short/Medium/Long). If space is tight, prefer clarity over literalness.
- Keep hashtags and usernames unchanged. Do not translate inside URLs.
- Use inclusive, gender-neutral phrasing where applicable.

Domain guidance:
- Safety and navigation strings must be unambiguous and action-oriented.
- Time- and event-related strings should read like clear, modern app copy.

Language-specific notes:
- ar (Arabic): Use Modern Standard Arabic, no diacritics. No added bidi or control characters. Concise imperative style.
- ko (Korean): Use natural UI Korean. Prefer concise imperative or noun-phrase UI form. Avoid unnecessary English loanwords when a standard Korean term exists.
- zh (Chinese): Use Simplified Chinese. Concise UI style. Avoid redundant particles.

Output:
- Return plain text for each translation (no surrounding quotes). Do NOT invent or omit keys.
"""            # extra guidance for the LLM

# ---- Verbose ---------------------------------------------------------------

VERBOSE = True
def vprint(*args, **kwargs):
    if VERBOSE:
        print(*args, **kwargs, file=sys.stderr)

# ---- Deps ------------------------------------------------------------------

# pip install lxml openai
from lxml import etree as ET  # preserves comments

try:
    from openai import OpenAI
except Exception:
    print("Missing OpenAI SDK. Install with: pip install openai", file=sys.stderr)
    raise

# ---- Helpers ---------------------------------------------------------------

def sha1(s: str) -> str:
    return hashlib.sha1(s.encode("utf-8")).hexdigest()

def read_xml_keep_comments(p: Path) -> ET._ElementTree:
    parser = ET.XMLParser(remove_blank_text=False, strip_cdata=False, remove_comments=False, encoding="utf-8")
    vprint(f"Parsing XML: {p}")
    return ET.parse(str(p), parser)

def write_xml(tree: ET._ElementTree, out_path: Path) -> None:
    out_path.parent.mkdir(parents=True, exist_ok=True)
    xml_bytes = ET.tostring(tree, xml_declaration=True, encoding="utf-8", pretty_print=True)
    out_path.write_bytes(xml_bytes)

def get_children_with_comments(resources_el: ET._Element) -> list:
    return [node for node in resources_el.iterchildren()]

def xml_text(node) -> str:
    return (node.text or "").strip()

def normalize_ws(s: str) -> str:
    return re.sub(r"\s+", " ", s or "").strip()

def nearest_preceding_comment_text(siblings: list, idx: int) -> str:
    j = idx - 1
    while j >= 0:
        n = siblings[j]
        if isinstance(n, ET._Comment):
            return normalize_ws(str(n))
        if isinstance(n.tag, str):
            break
        j -= 1
    return ""

def inline_trailing_comment(node) -> str:
    parent = node.getparent()
    if parent is None:
        return ""
    siblings = list(parent)
    i = siblings.index(node)
    if i + 1 < len(siblings) and isinstance(siblings[i + 1], ET._Comment):
        return normalize_ws(str(siblings[i + 1]))
    return ""

PLACEHOLDER_RE = re.compile(
    r"(%\d+\$[sdfe]|%\d+\$s|%s|%d|%f|\{\{[^}]+\}\})|(<[^>]+>)",
    flags=re.IGNORECASE
)

def extract_base_entries(base_tree: ET._ElementTree):
    root = base_tree.getroot()
    assert root.tag == "resources"
    sibs = get_children_with_comments(root)
    entries = []
    for i, n in enumerate(sibs):
        if isinstance(n, ET._Comment):
            continue
        if not isinstance(n.tag, str):
            continue
        if n.tag != "string":
            continue
        name = n.get("name")
        vprint(f"name: {name}")
        value = xml_text(n)
        before = nearest_preceding_comment_text(sibs, i)
        after = inline_trailing_comment(n)
        vprint(f"before: {before}")
        vprint(f"after: {after}")
        context = "\n".join([t for t in [before, after] if t])
        do_not_translate_ctx = bool(re.search(r"\b(do\s*not\s*translate|don[’']t\s*translate)\b", context, flags=re.I))
        do_not_translate_attr = (n.get("translatable") == "false")
        do_not_translate = do_not_translate_ctx or do_not_translate_attr
        src_hash = sha1(f"{name}\n{value}\n{context}")
        entries.append({
            "name": name,
            "value": value,
            "context": context,
            "do_not_translate": do_not_translate,
            "src_hash": src_hash,
            "placeholders": PLACEHOLDER_RE.findall(value),
        })
    return entries

def load_target(tree: ET._ElementTree):
    root = tree.getroot()
    map_by_name = {}
    sibs = get_children_with_comments(root)
    for i, n in enumerate(sibs):
        if isinstance(n, ET._Comment):
            continue
        if not isinstance(n.tag, str):
            continue
        if n.tag != "string":
            continue
        name = n.get("name")
        value = xml_text(n)
        before = nearest_preceding_comment_text(sibs, i)
        m = re.search(r"src_hash\s*:\s*([0-9a-f]{40})", before or "", flags=re.I)
        stored_hash = m.group(1) if m else None
        map_by_name[name] = {
            "node": n,
            "value": value,
            "stored_hash": stored_hash,
            "preceding_comment": before,
        }
    return map_by_name

def build_prompt_for_language(lang_code: str, items: list) -> str:
    language_label = {
        "fr": "French", "es": "Spanish", "de": "German",
        "ar": "Arabic", "ko": "Korean", "zh": "Chinese"
    }.get(lang_code, lang_code)

    instructions = f"""
You are translating UI strings for a mobile app. Target language: {language_label} ({lang_code}).

Global context of the application:
{GLOBAL_CONTEXT}

Translation consignes:
{TRANSLATION_CONSIGNS}

Rules:
- Output JSON ONLY. No prose. Schema: {{"translations":[{{"name": "...","text": "..."}}]}}
- Respect the tone of the source. Keep similar concision when context hints mention size.
- Preserve placeholders verbatim, including printf-style (%s, %1$s, %d), double-brace variables ({{var}}), and inline tags like <b>...</b>.
- Do not invent proper nouns. Follow consignes for cities/countries.
- If a string is already in the target language or marked do_not_translate, copy source.
- No extra quotes, bidi marks, or escaping beyond what is needed for JSON.
- Return plain text for Arabic/Korean/Chinese; no directional marks.
- Avoid leading/trailing whitespace unless present in source.
- The data comes from an XML file, so keep related concepts as double newlines in the translation if any

Translate these items:
"""
    payload = []
    for it in items:
        payload.append({
            "name": it["name"],
            "source": it["value"],
            "context": it["context"],
            "do_not_translate": it["do_not_translate"],
        })
    instructions += json.dumps(payload, ensure_ascii=False, indent=2)
    instructions += "\nReturn JSON only."
    return instructions.strip()

def call_openai_batch(lang: str, prompt: str) -> dict:
    client = OpenAI()
    vprint(f"[{lang}] Calling OpenAI model={MODEL}")
    try:
        resp = client.chat.completions.create(
            model=MODEL,
            messages=[{"role": "user", "content": prompt}],
            temperature=1,
            max_completion_tokens=40000,
            response_format={"type": "json_object"},
        )
        text = resp.choices[0].message.content
        vprint(f"[{lang}] OpenAI response received ({len(text)} chars)")
        return json.loads(text)
    except Exception as e:
        vprint(f"[{lang}] OpenAI call failed: {e}")
        raise

def rebuild_target_tree(base_entries, existing_map, translations_map, lang_code: str) -> (ET._ElementTree, dict):
    root = ET.Element("resources")
    changes = {"created": [], "modified": [], "unchanged": [], "deleted": []}

    t_by_name = {t["name"]: t["text"] for t in translations_map}

    def add_hash_comment(h):
        root.append(ET.Comment(f" src_hash: {h} "))

    base_names = []
    for it in base_entries:
        name = it["name"]
        base_names.append(name)
        base_val = it["value"]
        h = it["src_hash"]

        if it["do_not_translate"]:
            add_hash_comment(h)
            el = ET.SubElement(root, "string", name=name)
            el.text = base_val
            ex = existing_map.get(name)
            if ex and ex["value"] == base_val and ex["stored_hash"] == h:
                changes["unchanged"].append(name)
            elif ex:
                changes["modified"].append(name)
            else:
                changes["created"].append(name)
            continue

        translated = t_by_name.get(name, None)

        prev = existing_map.get(name)
        prev_hash = prev["stored_hash"] if prev else None
        prev_value = prev["value"] if prev else None

        if translated is None:
            out_text = prev_value if (prev and prev_hash == h) else (prev_value or base_val)
        else:
            out_text = translated

        add_hash_comment(h)
        el = ET.SubElement(root, "string", name=name)
        el.text = out_text

        if not prev:
            changes["created"].append(name)
        else:
            if prev_hash != h:
                if out_text != prev_value:
                    changes["modified"].append(name)
                else:
                    changes["modified"].append(name)
            else:
                if out_text == prev_value:
                    changes["unchanged"].append(name)
                else:
                    changes["modified"].append(name)

    for name in existing_map.keys():
        if name not in set(base_names):
            changes["deleted"].append(name)

    tree = ET.ElementTree(root)
    return tree, changes

def generate_language(
    lang: str,
    base_entries: list,
    base_root: Path,
    logs_dir: Path,
) -> None:
    target_dir = base_root / "shared" / "src" / "commonMain" / "moko-resources" / lang
    target_path = target_dir / "strings.xml"

    vprint(f"[{lang}] Target file: {target_path}")

    if target_path.exists():
        tgt_tree = read_xml_keep_comments(target_path)
        existing_map = load_target(tgt_tree)
    else:
        existing_map = {}

    work_items = []
    for it in base_entries:
        if it["do_not_translate"]:
            continue
        prev = existing_map.get(it["name"])
        needs = (prev is None) or (prev["stored_hash"] != it["src_hash"])
        if needs:
            work_items.append(it)

    vprint(f"[{lang}] Work items needing translation: {len(work_items)}")
    translations = []
    prompt = ""
    if work_items:
        prompt = build_prompt_for_language(lang, work_items)
        vprint(f"[{lang}] Sending one prompt with {len(work_items)} items")
        data = call_openai_batch(lang, prompt)
        translations = data.get("translations", [])
        have = {t["name"] for t in translations}
        missing = [it["name"] for it in work_items if it["name"] not in have]
        vprint(f"[{lang}] Received {len(translations)} translations, missing {len(missing)}")
        if missing:
            for m in missing:
                translations.append({"name": m, "text": next(e["value"] for e in work_items if e["name"] == m)})
    else:
        vprint(f"[{lang}] Nothing to translate. Rebuilding file to reflect base order and hashes.")

    tree, changes = rebuild_target_tree(base_entries, existing_map, translations, lang)
    write_xml(tree, target_path)
    vprint(f"[{lang}] Wrote translations to {target_path}")

    stamp = datetime.utcnow().strftime("%Y-%m-%d %H:%M:%S UTC")
    log_lines = [f"[{stamp}] Language={lang}"]
    for k in ("created", "modified", "unchanged", "deleted"):
        vals = sorted(set(changes[k]))
        log_lines.append(f"{k.upper()} ({len(vals)}): {', '.join(vals) if vals else '-'}")
    log_text = "\n".join(log_lines) + "\n\n" + prompt + "\n\n"
    logs_dir.mkdir(parents=True, exist_ok=True)
    (logs_dir / f"translation_{lang}.log").write_text(log_text, encoding="utf-8")
    vprint(f"[{lang}] Log written: {(logs_dir / f'translation_{lang}.log')}")

def main():
    ap = argparse.ArgumentParser(description="Translate Moko base strings.xml to target languages via OpenAI.")
    ap.add_argument("--base-root", default=".", help="Path to BASE_ROOT (defaults to current dir).")
    ap.add_argument("--languages", default=",".join(LANGUAGES), help="Comma-separated ISO codes.")
    ap.add_argument("--openai-api-key", default=os.getenv("OPENAI_API_KEY"), help="OpenAI API key (or set env).")
    ap.add_argument("--no-verbose", action="store_true", help="Print progress to stderr.")
    args = ap.parse_args()

    if not args.openai_api_key:
        print("Provide OPENAI_API_KEY via env or --openai-api-key.", file=sys.stderr)
        sys.exit(1)
    os.environ["OPENAI_API_KEY"] = args.openai_api_key
    global VERBOSE
    VERBOSE = not bool(args.no_verbose)

    base_root = (Path(args.base_root).resolve() / ".." / "..").resolve()
    vprint(f"Model: {MODEL}")
    vprint(f"Base root: {base_root}")

    base_path = base_root / "shared" / "src" / "commonMain" / "moko-resources" / "base" / "strings.xml"
    if not base_path.exists():
        print(f"Base strings.xml not found at: {base_path}", file=sys.stderr)
        sys.exit(1)

    base_tree = read_xml_keep_comments(base_path)
    base_entries = extract_base_entries(base_tree)
    vprint(f"Base entries: {len(base_entries)}")

    run_logs_dir = base_root / "shared" / "src" / "commonMain" / "moko-resources" / "logs"
    run_logs_dir.mkdir(parents=True, exist_ok=True)
    session_log = run_logs_dir / f"run_{datetime.utcnow().strftime('%Y%m%d_%H%M%S')}.txt"
    session_log.write_text("WorldWideWaves translation run started.\n", encoding="utf-8")

    langs = [x.strip() for x in args.languages.split(",") if x.strip()]
    vprint(f"Languages: {', '.join(langs)}")
    for lang in langs:
        generate_language(lang, base_entries, base_root, run_logs_dir)

    print("Done.")

if __name__ == "__main__":
    main()

