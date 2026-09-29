import math
from PIL import Image, ImageDraw

def create_realme_icon():
    # Create high-res 512x512 canvas
    size = (512, 512)
    img = Image.new("RGBA", size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Background rounded rectangle (Dark sleek slate with realme yellow accent)
    bg_color = (22, 24, 29, 255)
    border_color = (255, 199, 0, 255) # Realme Yellow
    
    # Draw outer squircle / rounded rect
    margin = 20
    corner_radius = 110
    draw.rounded_rectangle(
        [(margin, margin), (512 - margin, 512 - margin)],
        radius=corner_radius,
        fill=bg_color,
        outline=border_color,
        width=16
    )

    yellow = (255, 204, 0, 255)
    white = (245, 245, 250, 255)

    # Earbud Head Left
    draw.ellipse([(130, 160), (230, 260)], fill=white)
    # Stem Left
    draw.rounded_rectangle([(180, 230), (220, 365)], radius=18, fill=white)
    # Accent ring on stem
    draw.rounded_rectangle([(180, 280), (220, 298)], radius=6, fill=yellow)

    # Sound Waves / ANC Waves on the Right
    draw.arc([(240, 170), (330, 340)], start=300, end=60, fill=yellow, width=18)
    draw.arc([(280, 130), (410, 380)], start=305, end=55, fill=yellow, width=18)
    draw.arc([(320, 90), (485, 420)], start=310, end=50, fill=(255, 204, 0, 160), width=16)

    # Save PNG and multi-resolution ICO
    img.save("app_icon.png", format="PNG")
    
    # Generate ICO with standard Windows sizes
    icon_sizes = [(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)]
    img.save("app_icon.ico", format="ICO", sizes=icon_sizes)
    print("Icon generated successfully: app_icon.ico and app_icon.png")

if __name__ == "__main__":
    create_realme_icon()
