import os
from PIL import Image, ImageDraw, ImageFont

def main():
    width, height = 1024, 500
    # Background color matches Asteria's dark theme surface
    bg_color = (5, 8, 16) # #050810
    
    out = Image.new("RGBA", (width, height), bg_color)
    
    # Load icon parts
    bg = Image.open("app/src/main/res/mipmap-xxxhdpi/ic_launcher_background.webp").convert("RGBA")
    fg = Image.open("app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.webp").convert("RGBA")
    
    # Combine icon
    icon = Image.alpha_composite(bg, fg)
    
    # Resize icon (it is 432x432 usually)
    icon_size = 200
    icon = icon.resize((icon_size, icon_size), Image.Resampling.LANCZOS)
    
    # Text
    font_path = "app/src/main/res/font/space_grotesk_variable.ttf"
    font_size = 60
    font = ImageFont.truetype(font_path, font_size)
    text = "PROJECT ASTERIA"
    
    draw = ImageDraw.Draw(out)
    # Get text bounding box
    bbox = draw.textbbox((0, 0), text, font=font)
    text_width = bbox[2] - bbox[0]
    text_height = bbox[3] - bbox[1]
    
    # Spacing
    gap = 20
    total_height = icon_size + gap + text_height
    
    # Positions
    icon_x = (width - icon_size) // 2
    icon_y = (height - total_height) // 2
    
    text_x = (width - text_width) // 2
    text_y = icon_y + icon_size + gap
    
    # Paste icon
    out.paste(icon, (icon_x, icon_y), icon)
    
    # Draw text
    text_color = (255, 255, 255)
    draw.text((text_x, text_y), text, font=font, fill=text_color)
    
    # Save
    out.save("/home/orbit/.gemini/antigravity/brain/32dc9961-99ff-4e5c-beed-f69fc3ab059c/feature_graphic.png")
    print("Generated feature_graphic.png")

if __name__ == "__main__":
    main()
