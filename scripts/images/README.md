# Images

Image processing and asset generation tools for WorldWideWaves mobile app.

## Purpose

Handles image-related tasks:
- **Image optimization** and format conversion
- **City cover generation** for map previews
- **Asset preparation** for mobile platforms

## Structure

```
images/
├── generated_images/     # Output directory
├── city_covers.py       # Generate city cover images
├── convert_webp.sh      # Convert images to WebP format
└── README.md           # This file
```

## Usage

### Convert Images to WebP
```bash
# Convert all images in directory to WebP
./convert_webp.sh ../shared/src/commonMain/composeResources/drawable/

# Convert specific image
./convert_webp.sh image.png
```

### Generate City Covers
```bash
# Generate cover images for cities
python city_covers.py

# Custom city cover generation
python city_covers.py --city paris_france --style modern
```

## Tools

### WebP Conversion
The `convert_webp.sh` script converts PNG/JPEG images to WebP format for better compression while maintaining quality.

**Benefits:**
- ~30% smaller file sizes
- Better compression than PNG/JPEG
- Supported on modern Android/iOS

### City Cover Generation
The `city_covers.py` script generates promotional images for city maps using:
- City imagery and overlays
- Consistent branding and typography  
- Multiple size variants for different uses

## Configuration

### WebP Settings
```bash
# Quality settings (1-100)
WEBP_QUALITY=85

# Lossless mode for certain images
WEBP_LOSSLESS=true
```

### City Cover Settings
```python
# In city_covers.py
OUTPUT_SIZES = [(1920, 1080), (1200, 800), (800, 600)]
IMAGE_QUALITY = 90
FONT_FAMILY = "Roboto"
```

## Integration

### With App Resources
```bash
# Process shared resources
./convert_webp.sh ../shared/src/commonMain/composeResources/drawable/*.png

# Copy generated covers
cp generated_images/city_covers/* ../shared/src/commonMain/composeResources/drawable/
```

### Build Integration
```bash
# Add to build process
./gradlew processImages
# Runs image optimization as part of build
```

## Adding New Images

1. **Place source images** in appropriate input directory
2. **Run conversion scripts** to generate optimized versions
3. **Copy to app resources** directories
4. **Update app code** to reference new assets

## Requirements

### System Dependencies
```bash
# WebP tools
brew install webp

# Python for city covers
pip install Pillow requests
```

### Verification
```bash
# Check WebP support
cwebp -version

# Test Python dependencies
python -c "import PIL; print('PIL available')"
```

## Troubleshooting

### Common Issues
1. **WebP tools not found**: Install libwebp package
2. **Python errors**: Install required packages with pip
3. **Permission denied**: Make scripts executable with `chmod +x *.sh`
4. **Image quality issues**: Adjust quality settings in scripts