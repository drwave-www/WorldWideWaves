# Use the official Python image as a base
FROM python:3.11-slim

# Set environment variables
ENV PYTHONDONTWRITEBYTECODE 1
ENV PYTHONUNBUFFERED 1

# Set working directory
WORKDIR /app

# Install system utilities
RUN apt-get update && apt-get install -y  \
    procps  \
    psmisc \
    lsof

# Install dependencies
COPY requirements.txt /app/
RUN pip install --no-cache-dir -r requirements.txt
RUN pip uninstall -y opencv-python opencv-python-headless
RUN apt-get install -y build-essential cmake git python3-dev python3-numpy \
    libavcodec-dev libavformat-dev libswscale-dev \
    libgstreamer-plugins-base1.0-dev \
    libgstreamer1.0-dev libgtk-3-dev \
    libpng-dev libjpeg-dev libopenexr-dev libtiff-dev libwebp-dev \
    libopencv-dev x264 libx264-dev libssl-dev ffmpeg
RUN python -m pip install --no-binary opencv-python opencv-python
RUN rm -rf /var/lib/apt/lists/*

# Copy application files
COPY . /app/

# Expose the port that Flask runs on
EXPOSE 5000

# Run the Flask application
CMD ["gunicorn", "-w", "4", "-b", "0.0.0.0:5000", "--access-logfile", "/app/logs/access.log", "--error-logfile", "/app/logs/error.log", "--log-level", "INFO", "run:app"]