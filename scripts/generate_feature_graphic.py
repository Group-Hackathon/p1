import os
from PIL import Image, ImageDraw, ImageFont

# Dimensions required by Google Play Console
WIDTH, HEIGHT = 1024, 500

# Text to display
DESCRIPTION_TEXT = "Track daily symptoms and vitals to prepare your next medical appointment."

def main():
    # 1. Create a white background image
    img = Image.new("RGB", (WIDTH, HEIGHT), "white")
    draw = ImageDraw.Draw(img)

    # 2. Load and resize the logo
    logo_path = "playstore_logo_512.png"
    if os.path.exists(logo_path):
        try:
            logo = Image.open(logo_path)
            # Resize logo to a reasonable size, e.g., 200x200
            logo_size = 200
            logo = logo.resize((logo_size, logo_size), Image.Resampling.LANCZOS)
            
            # If the logo has an alpha channel, use it as a mask to preserve transparency
            if logo.mode in ('RGBA', 'LA') or (logo.mode == 'P' and 'transparency' in logo.info):
                # Paste logo centered horizontally, towards the top
                logo_x = (WIDTH - logo_size) // 2
                logo_y = 100
                img.paste(logo, (logo_x, logo_y), logo)
            else:
                logo_x = (WIDTH - logo_size) // 2
                logo_y = 100
                img.paste(logo, (logo_x, logo_y))
        except Exception as e:
            print(f"Error processing logo: {e}")
    else:
        print(f"Warning: Logo {logo_path} not found. Drawing a placeholder.")
        # Placeholder for logo
        draw.rectangle([(WIDTH//2 - 100, 100), (WIDTH//2 + 100, 300)], fill="gray")

    # 3. Load a font
    font_path = "/Library/Fonts/Arial.ttf" # Standard on macOS
    font_size = 32
    try:
        font = ImageFont.truetype(font_path, font_size)
    except IOError:
        print(f"Warning: Font {font_path} not found. Using default.")
        font = ImageFont.load_default()

    # 4. Draw the text on two lines, centered
    line1 = "Track daily symptoms and vitals"
    line2 = "to prepare your next medical appointment."
    
    def get_text_dims(text):
        try:
            bbox = draw.textbbox((0, 0), text, font=font)
            return bbox[2] - bbox[0], bbox[3] - bbox[1]
        except AttributeError:
            return draw.textsize(text, font=font)

    w1, h1 = get_text_dims(line1)
    w2, h2 = get_text_dims(line2)

    x1 = (WIDTH - w1) // 2
    y1 = 330
    
    x2 = (WIDTH - w2) // 2
    y2 = y1 + h1 + 10 # 10 pixels spacing between lines
    
    # Draw text in black
    draw.text((x1, y1), line1, fill="black", font=font)
    draw.text((x2, y2), line2, fill="black", font=font)

    # 5. Save the final image
    output_path = "feature_graphic_1024x500.png"
    img.save(output_path)
    print(f"Successfully generated {output_path} ({WIDTH}x{HEIGHT})")

if __name__ == "__main__":
    main()
