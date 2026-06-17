from PIL import Image, ImageDraw, ImageFont
import os

sizes = {
    "mdpi": 108,
    "hdpi": 162,
    "xhdpi": 216,
    "xxhdpi": 324,
    "xxxhdpi": 432
}

base_path = "/Users/gary/CATEGORIE-SOFTPOWER/p1/androidp1/app/src/main/res"

for density, size in sizes.items():
    img = Image.new('RGBA', (size, size), (255, 255, 255, 0))
    draw = ImageDraw.Draw(img)
    
    # Make text smaller: 42% of the icon size instead of 55%
    font_size = int(size * 0.42)
    try:
        font = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial Bold.ttf", font_size)
    except:
        font = ImageFont.load_default()
        
    text = "P1"
    
    # Get exact ink bounding box
    bbox = draw.textbbox((0, 0), text, font=font)
    w = bbox[2] - bbox[0]
    h = bbox[3] - bbox[1]
    
    # Perfect centering mathematically
    x = (size - w) / 2 - bbox[0]
    y = (size - h) / 2 - bbox[1]
    
    draw.text((x, y), text, fill='black', font=font)
    
    dir_path = os.path.join(base_path, f"mipmap-{density}-v26")
    os.makedirs(dir_path, exist_ok=True)
    img.save(os.path.join(dir_path, "ic_launcher_foreground.png"))
    
    # Legacy icon
    legacy_img = Image.new('RGB', (size, size), 'white')
    legacy_draw = ImageDraw.Draw(legacy_img)
    legacy_draw.text((x, y), text, fill='black', font=font)
    legacy_dir = os.path.join(base_path, f"mipmap-{density}")
    os.makedirs(legacy_dir, exist_ok=True)
    legacy_img.save(os.path.join(legacy_dir, "ic_launcher.png"))
    legacy_img.save(os.path.join(legacy_dir, "ic_launcher_round.png"))

print("Mipmaps generated perfectly centered and smaller.")
