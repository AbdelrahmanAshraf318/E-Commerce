"""
Generates the local-development catalogue:

  db/dev_seed_products.sql        categories, brands, products              (committed, small)
  db/dev_seed_product_images.sql  one photo per product in PRODUCT_IMAGE    (git-ignored, ~4 MB)
  db/PRODUCT_IMAGE_CREDITS.md     who took each photo                       (committed)

    python db/generate_dev_seed.py
    psql -h localhost -U postgres -d postgres -f db/dev_seed_products.sql -f db/dev_seed_product_images.sql

Photos come from Unsplash (https://unsplash.com/license - free to use, no attribution required; we credit anyway).
They are downloaded once into db/.image-cache/ (git-ignored), cropped server-side to 800x600 JPEG.
Both SQL files are idempotent; re-running the images file replaces images whose photo changed.
"""
import base64
import time
import urllib.error
import urllib.request
from pathlib import Path

HERE = Path(__file__).resolve().parent
CACHE = HERE / ".image-cache"
CATALOGUE_SQL = HERE / "dev_seed_products.sql"
IMAGES_SQL = HERE / "dev_seed_product_images.sql"
CREDITS = HERE / "PRODUCT_IMAGE_CREDITS.md"

# Unsplash's image CDN resizes/crops on request: 4:3 like the storefront cards, "entropy" keeps the busy part.
IMAGE_PARAMS = "w=800&h=600&fit=crop&crop=entropy&fm=jpg&q=72"

CATEGORIES = ["Electronics", "Books", "Home", "Sports & Outdoors", "Beauty"]
BRANDS = ["Acme", "Globex", "Initech", "Northwind", "Contoso"]

# (name, description, price, stock, status, category, brand, unsplash photo id, image base url, photographer)
PRODUCTS = [
    # --- Electronics ---
    ("Wireless Headphones", "Over-ear Bluetooth headphones with 30-hour battery and active noise cancelling.", 129.99, 25, "ACTIVE", "Electronics", "Acme",
     "PDX_a_82obo", "https://images.unsplash.com/photo-1505740420928-5e560c06d30e", "C D-X"),
    ("Mechanical Keyboard", "Tenkeyless keyboard with hot-swappable switches and PBT keycaps.", 89.50, 12, "ACTIVE", "Electronics", "Globex",
     "4GzqVNX0TCQ", "https://images.unsplash.com/photo-1632079003110-d694908500da", "JL Cabrera"),
    ("USB-C Charger 65W", "Compact GaN charger for laptops, tablets and phones.", 39.00, 0, "OUT_OF_STOCK", "Electronics", "Initech",
     "9QTFMkh-ezM", "https://images.unsplash.com/photo-1586254116951-5263e2cdb44c", "Markus Winkler"),
    ("Smartwatch Active 2", "Heart-rate, sleep and GPS tracking with a 7-day battery and always-on display.", 199.00, 4, "ACTIVE", "Electronics", "Northwind",
     "2wFoa040m8g", "https://images.unsplash.com/photo-1579586337278-3befd40fd17a", "Simon Daoudi"),
    ("Portable Bluetooth Speaker", "Waterproof (IP67) speaker with 360-degree sound and 20 hours of playback.", 59.99, 31, "ACTIVE", "Electronics", "Contoso",
     "QTsrvfTHvpA", "https://images.unsplash.com/photo-1675319245480-215961c129f1", "Tariq Mahmud Naim"),
    ("Ergonomic Wireless Mouse", "Silent clicks, 4000 DPI sensor and a contoured grip for long workdays.", 34.99, 48, "ACTIVE", "Electronics", "Globex",
     "ZtxED1cpB1E", "https://images.unsplash.com/photo-1527864550417-7fd91fc51a46", "Oscar Ivan Esquivel Arteaga"),
    ("Mirrorless Camera Z50", "24 MP APS-C sensor, 4K video and a flip-out touchscreen. Body only.", 849.00, 3, "ACTIVE", "Electronics", "Northwind",
     "lfRlv3nuf78", "https://images.unsplash.com/photo-1536632087471-3cf3f2986328", "Maddy"),
    ("10-inch Tablet", "Full-HD display, 128 GB storage and all-day battery for reading and streaming.", 279.00, 15, "ACTIVE", "Electronics", "Acme",
     "aeSSYYWY27w", "https://images.unsplash.com/photo-1604399852419-f67ee7d5f2ef", "Sanjeev Mohindra"),
    ("Wireless Game Controller", "Low-latency controller with rumble, works with PC, Android and consoles.", 49.95, 0, "OUT_OF_STOCK", "Electronics", "Contoso",
     "k4Akpt5-Sfk", "https://images.unsplash.com/photo-1552820728-8b83bb6b773f", "Alexey Savchenko"),
    # --- Books ---
    ("Clean Architecture", "A craftsman's guide to software structure and design.", 34.95, 40, "ACTIVE", "Books", "Globex",
     "HBEDoPQM6jM", "https://images.unsplash.com/photo-1589578641895-56f5e660549f", "Ayako"),
    ("Designing Data-Intensive Applications", "The big ideas behind reliable, scalable, and maintainable systems.", 49.99, 18, "ACTIVE", "Books", "Acme",
     "OzUcU_0uMTI", "https://images.unsplash.com/photo-1682357015040-1b45c90b8721", "Olya P"),
    ("Effective Java, 3rd Edition", "Best practices for the Java platform, from generics to lambdas and streams.", 44.99, 22, "ACTIVE", "Books", "Initech",
     "7hspi6m0yO4", "https://images.unsplash.com/photo-1549032305-07743102559d", "Kelly Sikkema"),
    ("Domain-Driven Design Distilled", "A concise introduction to bounded contexts, aggregates and strategic design.", 29.99, 5, "ACTIVE", "Books", "Northwind",
     "HXjtPt_XRAQ", "https://images.unsplash.com/photo-1485990005353-9abcf694f3e7", "Quilia"),
    # --- Home ---
    ("Ceramic Pour-Over Set", "Hand-glazed dripper with matching 600 ml carafe.", 42.00, 9, "ACTIVE", "Home", "Initech",
     "sK-pgy8W_Yc", "https://images.unsplash.com/photo-1631559964124-35d876a3180c", "Jamie Long"),
    ("Linen Throw Blanket", "Stonewashed linen, 130 x 170 cm.", 64.00, 7, "ACTIVE", "Home", "Acme",
     "VSRjzIj0148", "https://images.unsplash.com/photo-1674475760738-8c7af859f821", "Bearaby"),
    ("Discontinued Lamp", "Hidden from the storefront (IN_ACTIVE).", 15.00, 3, "IN_ACTIVE", "Home", "Globex",
     "pdIwPL3HU2s", "https://images.unsplash.com/photo-1517991104123-1d56a6e81ed9", "Joel Henry"),
    ("Dimmable Desk Lamp", "LED desk lamp with five colour temperatures and a USB charging port.", 38.50, 26, "ACTIVE", "Home", "Contoso",
     "mNXTZu7AeGA", "https://images.unsplash.com/photo-1570974802254-4b0ad1a755f5", "Brina Blum"),
    ("Indoor Herb Garden Kit", "Self-watering planter with basil, mint and parsley seed pods.", 27.00, 2, "ACTIVE", "Home", "Northwind",
     "ZchXTnNWCOM", "https://images.unsplash.com/photo-1553275991-b6ba99f234e1", "Sixteen Miles Out"),
    # --- Sports & Outdoors ---
    ("Trail Running Backpack 18L", "Lightweight pack with hydration sleeve, chest strap and reflective details.", 74.90, 14, "ACTIVE", "Sports & Outdoors", "Northwind",
     "8sjBzL1IyMo", "https://images.unsplash.com/photo-1509762774605-f07235a08f1f", "Josiah Weiss"),
    ("Urban Commuter Bike Light Set", "Rechargeable front and rear lights, 400 lumens, fits any handlebar.", 24.99, 60, "ACTIVE", "Sports & Outdoors", "Acme",
     "pwrLNxelrGU", "https://images.unsplash.com/photo-1579118690145-7753994c2d56", "Tower Electric Bikes"),
    ("Insulated Water Bottle 750ml", "Double-wall stainless steel keeps drinks cold for 24 hours.", 22.00, 85, "ACTIVE", "Sports & Outdoors", "Contoso",
     "OUjR8lrGccs", "https://images.unsplash.com/photo-1664714628878-9d2aa898b9e3", "personalgraphic.com"),
    ("Digital Sports Stopwatch", "Lap and split timing with a loud alarm and a lanyard.", 12.50, 33, "ACTIVE", "Sports & Outdoors", "Initech",
     "VwqMTcsb0Tg", "https://images.unsplash.com/photo-1704265586142-db3e17d0dea0", "William Warby"),
    # --- Beauty ---
    ("Mineral Sunscreen SPF 50", "Reef-friendly, fragrance-free daily sunscreen, 100 ml.", 18.99, 40, "ACTIVE", "Beauty", "Globex",
     "GxJ2vyVZZGI", "https://images.unsplash.com/photo-1714479140002-62d1824fffb5", "Point Normal"),
    ("Bamboo Makeup Brush Set", "Eight vegan brushes in a roll-up travel case.", 26.00, 11, "ACTIVE", "Beauty", "Contoso",
     "pxax5WuM7eY", "https://images.unsplash.com/photo-1516975080664-ed2fc6a32937", "Rosa Rafael"),
]


def sql(value: str) -> str:
    return "'" + value.replace("'", "''") + "'"


def photo_bytes(photo_id: str, base_url: str) -> bytes:
    """Downloads once, then reads from the cache. Validates it really is a JPEG."""
    CACHE.mkdir(exist_ok=True)
    cached = CACHE / f"{photo_id}.jpg"
    if not cached.exists():
        request = urllib.request.Request(f"{base_url}?{IMAGE_PARAMS}", headers={"User-Agent": "smartcart-dev-seed"})
        for attempt in range(1, 4):
            try:
                with urllib.request.urlopen(request, timeout=30) as response:
                    data = response.read()
                break
            except urllib.error.URLError as e:
                if attempt == 3:
                    raise
                print(f"  {photo_id}: {e.reason} - retrying")
                time.sleep(2 * attempt)
        if not data.startswith(b"\xff\xd8"):
            raise RuntimeError(f"{photo_id}: expected a JPEG, got {response.headers.get('Content-Type')}")
        cached.write_bytes(data)
        print(f"  downloaded {photo_id} ({len(data) // 1024} KB)")
    return cached.read_bytes()


def write_catalogue() -> None:
    lines = [
        "-- GENERATED by db/generate_dev_seed.py - edit the script, not this file.",
        "-- Sample catalogue for local development. Safe to run repeatedly.",
        "BEGIN;",
        "",
        "INSERT INTO category (category_name) VALUES " + ", ".join(f"({sql(c)})" for c in CATEGORIES)
        + " ON CONFLICT (category_name) DO NOTHING;",
        "INSERT INTO brand (brand_name) VALUES " + ", ".join(f"({sql(b)})" for b in BRANDS)
        + " ON CONFLICT (brand_name) DO NOTHING;",
        "",
    ]
    for name, description, price, stock, status, category, brand, *_ in PRODUCTS:
        lines.append(
            "INSERT INTO product (product_name, product_description, price, currency, stock_quantity, status, "
            "category_id, brand_id, created_at, updated_at)\n"
            f"SELECT {sql(name)}, {sql(description)}, {price:.2f}, 'USD', {stock}, {sql(status)},\n"
            f"       (SELECT category_id FROM category WHERE category_name = {sql(category)}),\n"
            f"       (SELECT brand_id FROM brand WHERE brand_name = {sql(brand)}), now(), now()\n"
            f"WHERE NOT EXISTS (SELECT 1 FROM product WHERE product_name = {sql(name)});"
        )
    lines += ["", "COMMIT;", ""]
    CATALOGUE_SQL.write_text("\n".join(lines), encoding="utf-8", newline="\n")


def write_images() -> int:
    lines = [
        "-- GENERATED by db/generate_dev_seed.py (git-ignored: it embeds the photos). Run dev_seed_products.sql first.",
        "BEGIN;",
        "",
    ]
    total = 0
    for name, *_, photo_id, base_url, _photographer in PRODUCTS:
        data = photo_bytes(photo_id, base_url)
        total += len(data)
        lines.append(
            "INSERT INTO product_image (product_id, content_type, image_data)\n"
            f"SELECT product_id, 'image/jpeg', decode('{base64.b64encode(data).decode('ascii')}', 'base64')\n"
            f"FROM product WHERE product_name = {sql(name)}\n"
            "ON CONFLICT (product_id) DO UPDATE SET content_type = EXCLUDED.content_type, image_data = EXCLUDED.image_data;"
        )
    lines += ["", "COMMIT;", ""]
    IMAGES_SQL.write_text("\n".join(lines), encoding="utf-8", newline="\n")
    return total


def write_credits() -> None:
    lines = [
        "# Product image credits",
        "",
        "Sample product photos are from [Unsplash](https://unsplash.com) under the "
        "[Unsplash License](https://unsplash.com/license). Generated by `generate_dev_seed.py`.",
        "",
        "| Product | Photo | Photographer |",
        "|---|---|---|",
    ]
    for name, *_, photo_id, _base_url, photographer in PRODUCTS:
        lines.append(f"| {name} | [unsplash.com/photos/{photo_id}](https://unsplash.com/photos/{photo_id}) | {photographer} |")
    CREDITS.write_text("\n".join(lines) + "\n", encoding="utf-8", newline="\n")


def main() -> None:
    write_catalogue()
    total = write_images()
    write_credits()
    print(f"{len(PRODUCTS)} products, {len(CATEGORIES)} categories, {len(BRANDS)} brands, "
          f"{total // 1024} KB of photos -> {CATALOGUE_SQL.name}, {IMAGES_SQL.name}")


if __name__ == "__main__":
    main()
