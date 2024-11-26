# Use the official Python image as a base
FROM python:3.11-slim

# Set environment variables
ENV CRYPTOGRAPHY_DONT_BUILD_RUST 1
ENV PYTHONDONTWRITEBYTECODE 1
ENV PYTHONUNBUFFERED 1

# Set working directory
WORKDIR /app

# Install system utilities
RUN apt-get update && apt-get install -y  \
    procps  \
    psmisc \
    lsof

# Install system dependencies
RUN apt-get install -y build-essential cmake git python3-dev python3-numpy \
    libavcodec-dev libavformat-dev libswscale-dev \
    libgstreamer-plugins-base1.0-dev \
    libgstreamer1.0-dev libgtk-3-dev curl \
    libpng-dev libjpeg-dev libopenexr-dev libtiff-dev libwebp-dev \
    gcc g++ make espeak-ng libsndfile1-dev libespeak-dev \
    libopencv-dev x264 libx264-dev libssl-dev ffmpeg

RUN rm -rf /var/lib/apt/lists/*

RUN curl https://sh.rustup.rs -sSf | sh -s -- --default-toolchain 1.82.0 -y
ENV PATH=$PATH:/root/.cargo/bin
ENV RUSTUP_TOOLCHAIN=1.82.0

# Install python dependencies
COPY requirements.txt /app/
RUN pip install --upgrade pip

RUN pip install --no-cache-dir --no-input -r requirements.txt

RUN pip uninstall -y opencv-python opencv-python-headless
RUN python -m pip install --no-binary opencv-python opencv-python

RUN python -m unidic download

# Copy application files
COPY . /app/

# Expose the port that Flask runs on
EXPOSE 5000

# Run the Flask application
CMD ["gunicorn", "-w", "4", "-b", "0.0.0.0:5000", "--access-logfile", "/app/logs/access.log", "--error-logfile", "/app/logs/error.log", "--log-level", "INFO", "run:app"]
