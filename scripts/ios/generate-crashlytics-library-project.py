# Copyright 2025 DrWave
#
# WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
# countries. The project aims to transcend physical and cultural
# boundaries, fostering unity, community, and shared human experience by leveraging real-time
# coordination and location-based services.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#!/usr/bin/env python3
"""
Generate CrashlyticsBridge static library Xcode project programmatically.

This script creates a complete, production-ready Xcode project for the CrashlyticsBridge
static library. The library is needed because iOS apps cannot export Objective-C symbols
for Kotlin/Native frameworks to import at runtime.

Architecture:
    libCrashlyticsBridge.a (this project)
          ↓ linked at framework build time
    Shared.framework (Kotlin/Native)
          ↓ linked at app build time
    iOS App

This ensures the Objective-C symbol _OBJC_CLASS_$_CrashlyticsBridge is available
when Shared.framework needs it, avoiding the "symbol not found" crash.

Usage:
    python3 generate-crashlytics-library-project.py

Output:
    iosApp/CrashlyticsBridge/CrashlyticsBridge.xcodeproj/project.pbxproj
"""

import os
import uuid
import plistlib
from pathlib import Path

# Project paths
SCRIPT_DIR = Path(__file__).parent
PROJECT_ROOT = SCRIPT_DIR.parent.parent
IOS_APP_DIR = PROJECT_ROOT / "iosApp"
BRIDGE_DIR = IOS_APP_DIR / "CrashlyticsBridge"
BRIDGE_PROJ_DIR = BRIDGE_DIR / "CrashlyticsBridge.xcodeproj"

# Generate stable UUIDs for project objects
def stable_uuid(seed: str) -> str:
    """Generate a stable 24-character hex UUID from a seed string."""
    import hashlib
    hash_obj = hashlib.sha256(seed.encode())
    return hash_obj.hexdigest()[:24].upper()

# Project structure UUIDs (stable based on names)
UUIDS = {
    'project': stable_uuid('CrashlyticsBridge_project'),
    'library_target': stable_uuid('CrashlyticsBridge_target'),
    'test_target': stable_uuid('CrashlyticsBridgeTests_target'),
    'library_product': stable_uuid('libCrashlyticsBridge.a'),
    'test_product': stable_uuid('CrashlyticsBridgeTests.xctest'),
    'sources_phase': stable_uuid('Sources_BuildPhase'),
    'frameworks_phase': stable_uuid('Frameworks_BuildPhase'),
    'test_sources_phase': stable_uuid('TestSources_BuildPhase'),
    'test_frameworks_phase': stable_uuid('TestFrameworks_BuildPhase'),
    'crashlytics_h': stable_uuid('CrashlyticsBridge.h_fileref'),
    'crashlytics_m': stable_uuid('CrashlyticsBridge.m_fileref'),
    'crashlytics_h_build': stable_uuid('CrashlyticsBridge.h_buildfile'),
    'crashlytics_m_build': stable_uuid('CrashlyticsBridge.m_buildfile'),
    'tests_m': stable_uuid('CrashlyticsBridgeTests.m_fileref'),
    'tests_m_build': stable_uuid('CrashlyticsBridgeTests.m_buildfile'),
    'main_group': stable_uuid('MainGroup'),
    'products_group': stable_uuid('ProductsGroup'),
    'crashlytics_group': stable_uuid('CrashlyticsBridgeGroup'),
    'tests_group': stable_uuid('TestsGroup'),
    'debug_config': stable_uuid('Debug_Config'),
    'release_config': stable_uuid('Release_Config'),
    'project_config_list': stable_uuid('Project_ConfigList'),
    'target_config_list': stable_uuid('Target_ConfigList'),
    'test_config_list': stable_uuid('Test_ConfigList'),
    'firebase_package': stable_uuid('Firebase_Package'),
    'firebase_crashlytics': stable_uuid('FirebaseCrashlytics_Product'),
}

def generate_project_pbxproj():
    """Generate complete project.pbxproj content."""

    project = {
        'archiveVersion': '1',
        'classes': {},
        'objectVersion': '77',
        'objects': {},
        'rootObject': UUIDS['project']
    }

    objects = project['objects']

    # PBXBuildFile section
    objects[UUIDS['crashlytics_h_build']] = {
        'isa': 'PBXBuildFile',
        'fileRef': UUIDS['crashlytics_h']
    }
    objects[UUIDS['crashlytics_m_build']] = {
        'isa': 'PBXBuildFile',
        'fileRef': UUIDS['crashlytics_m']
    }
    objects[UUIDS['tests_m_build']] = {
        'isa': 'PBXBuildFile',
        'fileRef': UUIDS['tests_m']
    }

    # PBXFileReference section
    objects[UUIDS['library_product']] = {
        'isa': 'PBXFileReference',
        'explicitFileType': 'archive.ar',
        'includeInIndex': '0',
        'path': 'libCrashlyticsBridge.a',
        'sourceTree': 'BUILT_PRODUCTS_DIR'
    }
    objects[UUIDS['test_product']] = {
        'isa': 'PBXFileReference',
        'explicitFileType': 'wrapper.cfbundle',
        'includeInIndex': '0',
        'path': 'CrashlyticsBridgeTests.xctest',
        'sourceTree': 'BUILT_PRODUCTS_DIR'
    }
    objects[UUIDS['crashlytics_h']] = {
        'isa': 'PBXFileReference',
        'fileEncoding': '4',
        'lastKnownFileType': 'sourcecode.c.h',
        'path': 'CrashlyticsBridge.h',
        'sourceTree': '<group>'
    }
    objects[UUIDS['crashlytics_m']] = {
        'isa': 'PBXFileReference',
        'fileEncoding': '4',
        'lastKnownFileType': 'sourcecode.c.objc',
        'path': 'CrashlyticsBridge.m',
        'sourceTree': '<group>'
    }
    objects[UUIDS['tests_m']] = {
        'isa': 'PBXFileReference',
        'fileEncoding': '4',
        'lastKnownFileType': 'sourcecode.c.objc',
        'path': 'CrashlyticsBridgeTests.m',
        'sourceTree': '<group>'
    }

    # Groups
    objects[UUIDS['main_group']] = {
        'isa': 'PBXGroup',
        'children': [
            UUIDS['crashlytics_group'],
            UUIDS['tests_group'],
            UUIDS['products_group']
        ],
        'sourceTree': '<group>'
    }
    objects[UUIDS['products_group']] = {
        'isa': 'PBXGroup',
        'children': [
            UUIDS['library_product'],
            UUIDS['test_product']
        ],
        'name': 'Products',
        'sourceTree': '<group>'
    }
    objects[UUIDS['crashlytics_group']] = {
        'isa': 'PBXGroup',
        'children': [
            UUIDS['crashlytics_h'],
            UUIDS['crashlytics_m']
        ],
        'path': 'CrashlyticsBridge',
        'sourceTree': '<group>'
    }
    objects[UUIDS['tests_group']] = {
        'isa': 'PBXGroup',
        'children': [
            UUIDS['tests_m']
        ],
        'path': 'CrashlyticsBridgeTests',
        'sourceTree': '<group>'
    }

    # Build phases
    objects[UUIDS['sources_phase']] = {
        'isa': 'PBXSourcesBuildPhase',
        'buildActionMask': '2147483647',
        'files': [
            UUIDS['crashlytics_m_build']
        ],
        'runOnlyForDeploymentPostprocessing': '0'
    }
    objects[UUIDS['frameworks_phase']] = {
        'isa': 'PBXFrameworksBuildPhase',
        'buildActionMask': '2147483647',
        'files': [],
        'runOnlyForDeploymentPostprocessing': '0'
    }
    objects[UUIDS['test_sources_phase']] = {
        'isa': 'PBXSourcesBuildPhase',
        'buildActionMask': '2147483647',
        'files': [
            UUIDS['tests_m_build']
        ],
        'runOnlyForDeploymentPostprocessing': '0'
    }

    # Native targets
    objects[UUIDS['library_target']] = {
        'isa': 'PBXNativeTarget',
        'buildConfigurationList': UUIDS['target_config_list'],
        'buildPhases': [
            UUIDS['sources_phase'],
            UUIDS['frameworks_phase']
        ],
        'buildRules': [],
        'dependencies': [],
        'name': 'CrashlyticsBridge',
        'packageProductDependencies': [
            UUIDS['firebase_crashlytics']
        ],
        'productName': 'CrashlyticsBridge',
        'productReference': UUIDS['library_product'],
        'productType': 'com.apple.product-type.library.static'
    }

    objects[UUIDS['test_target']] = {
        'isa': 'PBXNativeTarget',
        'buildConfigurationList': UUIDS['test_config_list'],
        'buildPhases': [
            UUIDS['test_sources_phase']
        ],
        'buildRules': [],
        'dependencies': [],
        'name': 'CrashlyticsBridgeTests',
        'packageProductDependencies': [
            UUIDS['firebase_crashlytics']
        ],
        'productName': 'CrashlyticsBridgeTests',
        'productReference': UUIDS['test_product'],
        'productType': 'com.apple.product-type.bundle.unit-test'
    }

    # Project object
    objects[UUIDS['project']] = {
        'isa': 'PBXProject',
        'attributes': {
            'BuildIndependentTargetsInParallel': 'YES',
            'LastUpgradeCheck': '1640',
            'TargetAttributes': {
                UUIDS['library_target']: {
                    'CreatedOnToolsVersion': '16.4.0'
                },
                UUIDS['test_target']: {
                    'CreatedOnToolsVersion': '16.4.0',
                    'TestTargetID': UUIDS['library_target']
                }
            }
        },
        'buildConfigurationList': UUIDS['project_config_list'],
        'compatibilityVersion': 'Xcode 15.3',
        'developmentRegion': 'en',
        'hasScannedForEncodings': '0',
        'knownRegions': ['en', 'Base'],
        'mainGroup': UUIDS['main_group'],
        'packageReferences': [
            UUIDS['firebase_package']
        ],
        'productRefGroup': UUIDS['products_group'],
        'projectDirPath': '',
        'projectRoot': '',
        'targets': [
            UUIDS['library_target'],
            UUIDS['test_target']
        ]
    }

    # Firebase package reference
    objects[UUIDS['firebase_package']] = {
        'isa': 'XCRemoteSwiftPackageReference',
        'repositoryURL': 'https://github.com/firebase/firebase-ios-sdk',
        'requirement': {
            'kind': 'upToNextMajorVersion',
            'minimumVersion': '12.4.0'
        }
    }

    objects[UUIDS['firebase_crashlytics']] = {
        'isa': 'XCSwiftPackageProductDependency',
        'package': UUIDS['firebase_package'],
        'productName': 'FirebaseCrashlytics'
    }

    # Build configurations
    objects[UUIDS['debug_config']] = {
        'isa': 'XCBuildConfiguration',
        'buildSettings': {
            'ALWAYS_SEARCH_USER_PATHS': 'NO',
            'ASSETCATALOG_COMPILER_GENERATE_SWIFT_ASSET_SYMBOL_EXTENSIONS': 'YES',
            'CLANG_ANALYZER_NONNULL': 'YES',
            'CLANG_ANALYZER_NUMBER_OBJECT_CONVERSION': 'YES_AGGRESSIVE',
            'CLANG_CXX_LANGUAGE_STANDARD': 'gnu++20',
            'CLANG_ENABLE_MODULES': 'YES',
            'CLANG_ENABLE_OBJC_ARC': 'YES',
            'CLANG_WARN_BLOCK_CAPTURE_AUTORELEASING': 'YES',
            'CLANG_WARN_BOOL_CONVERSION': 'YES',
            'CLANG_WARN_COMMA': 'YES',
            'CLANG_WARN_CONSTANT_CONVERSION': 'YES',
            'CLANG_WARN_DEPRECATED_OBJC_IMPLEMENTATIONS': 'YES',
            'CLANG_WARN_DIRECT_OBJC_ISA_USAGE': 'YES_ERROR',
            'CLANG_WARN_DOCUMENTATION_COMMENTS': 'YES',
            'CLANG_WARN_EMPTY_BODY': 'YES',
            'CLANG_WARN_ENUM_CONVERSION': 'YES',
            'CLANG_WARN_INFINITE_RECURSION': 'YES',
            'CLANG_WARN_INT_CONVERSION': 'YES',
            'CLANG_WARN_NON_LITERAL_NULL_CONVERSION': 'YES',
            'CLANG_WARN_OBJC_IMPLICIT_RETAIN_SELF': 'YES',
            'CLANG_WARN_OBJC_LITERAL_CONVERSION': 'YES',
            'CLANG_WARN_OBJC_ROOT_CLASS': 'YES_ERROR',
            'CLANG_WARN_QUOTED_INCLUDE_IN_FRAMEWORK_HEADER': 'YES',
            'CLANG_WARN_RANGE_LOOP_ANALYSIS': 'YES',
            'CLANG_WARN_STRICT_PROTOTYPES': 'YES',
            'CLANG_WARN_SUSPICIOUS_MOVE': 'YES',
            'CLANG_WARN_UNGUARDED_AVAILABILITY': 'YES_AGGRESSIVE',
            'CLANG_WARN_UNREACHABLE_CODE': 'YES',
            'CLANG_WARN__DUPLICATE_METHOD_MATCH': 'YES',
            'COPY_PHASE_STRIP': 'NO',
            'DEBUG_INFORMATION_FORMAT': 'dwarf',
            'ENABLE_STRICT_OBJC_MSGSEND': 'YES',
            'ENABLE_TESTABILITY': 'YES',
            'ENABLE_USER_SCRIPT_SANDBOXING': 'YES',
            'GCC_C_LANGUAGE_STANDARD': 'gnu17',
            'GCC_DYNAMIC_NO_PIC': 'NO',
            'GCC_NO_COMMON_BLOCKS': 'YES',
            'GCC_OPTIMIZATION_LEVEL': '0',
            'GCC_PREPROCESSOR_DEFINITIONS': ['DEBUG=1', '$(inherited)'],
            'GCC_WARN_64_TO_32_BIT_CONVERSION': 'YES',
            'GCC_WARN_ABOUT_RETURN_TYPE': 'YES_ERROR',
            'GCC_WARN_UNDECLARED_SELECTOR': 'YES',
            'GCC_WARN_UNINITIALIZED_AUTOS': 'YES_AGGRESSIVE',
            'GCC_WARN_UNUSED_FUNCTION': 'YES',
            'GCC_WARN_UNUSED_VARIABLE': 'YES',
            'IPHONEOS_DEPLOYMENT_TARGET': '16.0',
            'LOCALIZATION_PREFERS_STRING_CATALOGS': 'YES',
            'MTL_ENABLE_DEBUG_INFO': 'INCLUDE_SOURCE',
            'MTL_FAST_MATH': 'YES',
            'ONLY_ACTIVE_ARCH': 'YES',
            'SDKROOT': 'iphoneos',
            'SWIFT_ACTIVE_COMPILATION_CONDITIONS': 'DEBUG',
            'SWIFT_OPTIMIZATION_LEVEL': '-Onone'
        },
        'name': 'Debug'
    }

    objects[UUIDS['release_config']] = {
        'isa': 'XCBuildConfiguration',
        'buildSettings': {
            'ALWAYS_SEARCH_USER_PATHS': 'NO',
            'ASSETCATALOG_COMPILER_GENERATE_SWIFT_ASSET_SYMBOL_EXTENSIONS': 'YES',
            'CLANG_ANALYZER_NONNULL': 'YES',
            'CLANG_ANALYZER_NUMBER_OBJECT_CONVERSION': 'YES_AGGRESSIVE',
            'CLANG_CXX_LANGUAGE_STANDARD': 'gnu++20',
            'CLANG_ENABLE_MODULES': 'YES',
            'CLANG_ENABLE_OBJC_ARC': 'YES',
            'CLANG_WARN_BLOCK_CAPTURE_AUTORELEASING': 'YES',
            'CLANG_WARN_BOOL_CONVERSION': 'YES',
            'CLANG_WARN_COMMA': 'YES',
            'CLANG_WARN_CONSTANT_CONVERSION': 'YES',
            'CLANG_WARN_DEPRECATED_OBJC_IMPLEMENTATIONS': 'YES',
            'CLANG_WARN_DIRECT_OBJC_ISA_USAGE': 'YES_ERROR',
            'CLANG_WARN_DOCUMENTATION_COMMENTS': 'YES',
            'CLANG_WARN_EMPTY_BODY': 'YES',
            'CLANG_WARN_ENUM_CONVERSION': 'YES',
            'CLANG_WARN_INFINITE_RECURSION': 'YES',
            'CLANG_WARN_INT_CONVERSION': 'YES',
            'CLANG_WARN_NON_LITERAL_NULL_CONVERSION': 'YES',
            'CLANG_WARN_OBJC_IMPLICIT_RETAIN_SELF': 'YES',
            'CLANG_WARN_OBJC_LITERAL_CONVERSION': 'YES',
            'CLANG_WARN_OBJC_ROOT_CLASS': 'YES_ERROR',
            'CLANG_WARN_QUOTED_INCLUDE_IN_FRAMEWORK_HEADER': 'YES',
            'CLANG_WARN_RANGE_LOOP_ANALYSIS': 'YES',
            'CLANG_WARN_STRICT_PROTOTYPES': 'YES',
            'CLANG_WARN_SUSPICIOUS_MOVE': 'YES',
            'CLANG_WARN_UNGUARDED_AVAILABILITY': 'YES_AGGRESSIVE',
            'CLANG_WARN_UNREACHABLE_CODE': 'YES',
            'CLANG_WARN__DUPLICATE_METHOD_MATCH': 'YES',
            'COPY_PHASE_STRIP': 'NO',
            'DEBUG_INFORMATION_FORMAT': 'dwarf-with-dsym',
            'ENABLE_NS_ASSERTIONS': 'NO',
            'ENABLE_STRICT_OBJC_MSGSEND': 'YES',
            'ENABLE_USER_SCRIPT_SANDBOXING': 'YES',
            'GCC_C_LANGUAGE_STANDARD': 'gnu17',
            'GCC_NO_COMMON_BLOCKS': 'YES',
            'GCC_WARN_64_TO_32_BIT_CONVERSION': 'YES',
            'GCC_WARN_ABOUT_RETURN_TYPE': 'YES_ERROR',
            'GCC_WARN_UNDECLARED_SELECTOR': 'YES',
            'GCC_WARN_UNINITIALIZED_AUTOS': 'YES_AGGRESSIVE',
            'GCC_WARN_UNUSED_FUNCTION': 'YES',
            'GCC_WARN_UNUSED_VARIABLE': 'YES',
            'IPHONEOS_DEPLOYMENT_TARGET': '16.0',
            'LOCALIZATION_PREFERS_STRING_CATALOGS': 'YES',
            'MTL_ENABLE_DEBUG_INFO': 'NO',
            'MTL_FAST_MATH': 'YES',
            'SDKROOT': 'iphoneos',
            'SWIFT_COMPILATION_MODE': 'wholemodule',
            'VALIDATE_PRODUCT': 'YES'
        },
        'name': 'Release'
    }

    # Configuration lists
    objects[UUIDS['project_config_list']] = {
        'isa': 'XCConfigurationList',
        'buildConfigurations': [
            UUIDS['debug_config'],
            UUIDS['release_config']
        ],
        'defaultConfigurationIsVisible': '0',
        'defaultConfigurationName': 'Release'
    }

    # Target-specific configurations
    target_debug = UUIDS['debug_config'] + '_target'
    target_release = UUIDS['release_config'] + '_target'

    objects[target_debug] = {
        'isa': 'XCBuildConfiguration',
        'buildSettings': {
            'CODE_SIGN_STYLE': 'Automatic',
            'CURRENT_PROJECT_VERSION': '1',
            'DEFINES_MODULE': 'YES',
            'DYLIB_COMPATIBILITY_VERSION': '1',
            'DYLIB_CURRENT_VERSION': '1',
            'EXECUTABLE_PREFIX': 'lib',
            'GENERATE_INFOPLIST_FILE': 'YES',
            'MARKETING_VERSION': '1.0',
            'OTHER_LDFLAGS': '',
            'PRODUCT_NAME': '$(TARGET_NAME)',
            'SKIP_INSTALL': 'YES',
            'SUPPORTED_PLATFORMS': 'iphoneos iphonesimulator',
            'SUPPORTS_MACCATALYST': 'NO',
            'TARGETED_DEVICE_FAMILY': '1,2'
        },
        'name': 'Debug'
    }

    objects[target_release] = {
        'isa': 'XCBuildConfiguration',
        'buildSettings': {
            'CODE_SIGN_STYLE': 'Automatic',
            'CURRENT_PROJECT_VERSION': '1',
            'DEFINES_MODULE': 'YES',
            'DYLIB_COMPATIBILITY_VERSION': '1',
            'DYLIB_CURRENT_VERSION': '1',
            'EXECUTABLE_PREFIX': 'lib',
            'GENERATE_INFOPLIST_FILE': 'YES',
            'MARKETING_VERSION': '1.0',
            'OTHER_LDFLAGS': '',
            'PRODUCT_NAME': '$(TARGET_NAME)',
            'SKIP_INSTALL': 'YES',
            'SUPPORTED_PLATFORMS': 'iphoneos iphonesimulator',
            'SUPPORTS_MACCATALYST': 'NO',
            'TARGETED_DEVICE_FAMILY': '1,2'
        },
        'name': 'Release'
    }

    objects[UUIDS['target_config_list']] = {
        'isa': 'XCConfigurationList',
        'buildConfigurations': [
            target_debug,
            target_release
        ],
        'defaultConfigurationIsVisible': '0',
        'defaultConfigurationName': 'Release'
    }

    # Test configuration
    test_debug = UUIDS['debug_config'] + '_test'
    test_release = UUIDS['release_config'] + '_test'

    objects[test_debug] = {
        'isa': 'XCBuildConfiguration',
        'buildSettings': {
            'CODE_SIGN_STYLE': 'Automatic',
            'CURRENT_PROJECT_VERSION': '1',
            'GENERATE_INFOPLIST_FILE': 'YES',
            'MARKETING_VERSION': '1.0',
            'PRODUCT_BUNDLE_IDENTIFIER': 'com.worldwidewaves.CrashlyticsBridgeTests',
            'PRODUCT_NAME': '$(TARGET_NAME)',
            'SUPPORTED_PLATFORMS': 'iphoneos iphonesimulator',
            'TARGETED_DEVICE_FAMILY': '1,2'
        },
        'name': 'Debug'
    }

    objects[test_release] = {
        'isa': 'XCBuildConfiguration',
        'buildSettings': {
            'CODE_SIGN_STYLE': 'Automatic',
            'CURRENT_PROJECT_VERSION': '1',
            'GENERATE_INFOPLIST_FILE': 'YES',
            'MARKETING_VERSION': '1.0',
            'PRODUCT_BUNDLE_IDENTIFIER': 'com.worldwidewaves.CrashlyticsBridgeTests',
            'PRODUCT_NAME': '$(TARGET_NAME)',
            'SUPPORTED_PLATFORMS': 'iphoneos iphonesimulator',
            'TARGETED_DEVICE_FAMILY': '1,2'
        },
        'name': 'Release'
    }

    objects[UUIDS['test_config_list']] = {
        'isa': 'XCConfigurationList',
        'buildConfigurations': [
            test_debug,
            test_release
        ],
        'defaultConfigurationIsVisible': '0',
        'defaultConfigurationName': 'Release'
    }

    return project

def write_pbxproj(project, output_path):
    """Write project dict as old-style ASCII plist (required by Xcode)."""
    # Xcode project.pbxproj must be in old ASCII plist format
    output_path.parent.mkdir(parents=True, exist_ok=True)

    # Convert to ASCII plist format manually (plistlib writes XML by default)
    with open(output_path, 'w') as f:
        f.write('// !$*UTF8*$!\n')
        f.write(dict_to_plist(project, 0))

def dict_to_plist(obj, indent=0):
    """Convert dict to ASCII plist format."""
    tab = '\t' * indent

    if isinstance(obj, dict):
        lines = ['{\n']
        for key, value in obj.items():
            lines.append(f'{tab}\t\t{key} = {dict_to_plist(value, indent + 1)};\n')
        lines.append(f'{tab}\t}}')
        return ''.join(lines)
    elif isinstance(obj, list):
        lines = ['(\n']
        for item in obj:
            lines.append(f'{tab}\t\t{dict_to_plist(item, indent + 1)},\n')
        lines.append(f'{tab}\t)')
        return ''.join(lines)
    elif isinstance(obj, str):
        # Quote strings if they contain spaces or special chars
        if ' ' in obj or any(c in obj for c in '/$()'):
            return f'"{obj}"'
        return obj
    else:
        return str(obj)

def main():
    print("=" * 80)
    print("CrashlyticsBridge Static Library Project Generator")
    print("=" * 80)
    print()

    # Generate project
    print("1. Generating project.pbxproj...")
    project = generate_project_pbxproj()

    # Write to file
    pbxproj_path = BRIDGE_PROJ_DIR / 'project.pbxproj'
    print(f"2. Writing to {pbxproj_path}...")
    write_pbxproj(project, pbxproj_path)

    print()
    print("✅ Project generated successfully!")
    print()
    print("Next steps:")
    print("  1. Open in Xcode: open", str(BRIDGE_PROJ_DIR.parent / 'CrashlyticsBridge.xcodeproj'))
    print("  2. Verify Firebase dependency resolved")
    print("  3. Build: xcodebuild -project ... -target CrashlyticsBridge build")
    print()

if __name__ == '__main__':
    main()
