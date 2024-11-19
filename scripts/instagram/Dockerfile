# Use the official Python image as a base
FROM python:3.9-slim

# Set environment variables
ENV PYTHONDONTWRITEBYTECODE 1
ENV PYTHONUNBUFFERED 1

# Set working directory
WORKDIR /app

# Copy requirements file
COPY requirements.txt /app/

# Install system utilities
RUN apt-get update && apt-get install -y procps psmisc lsof && rm -rf /var/lib/apt/lists/*

# Install dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Copy application files
COPY . /app/

# Expose the port that Flask runs on
EXPOSE 5000

# Run the Flask application
CMD ["gunicorn", "-w", "4", "-b", "0.0.0.0:5000", "--access-logfile", "/app/logs/access.log", "--error-logfile", "/app/logs/error.log", "--log-level", "INFO", "app:app"]

