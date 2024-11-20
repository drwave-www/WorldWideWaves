# Build MELO Docker
git clone https://github.com/myshell-ai/MeloTTS.git
cd MeloTTS
cat << EOF >> requirements.txt
botocore==1.34.98
cached_path==1.6.2
EOF
docker build -t melotts . 
