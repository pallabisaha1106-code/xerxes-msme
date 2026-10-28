# Sample bulk-upload CSVs

Ready-to-use sample data for testing the bulk upload feature (Categories, Vendors,
Customers, Products — each list page has a **Download Sample CSV** / **Upload CSV**
pair of buttons). These files match exactly what each page's "Download Sample CSV"
button produces, so you can either use these directly or download fresh ones from
the app.

Scenario: a small apparel shop in India selling men's and women's clothing.

- **categories-sample.csv** — Men's Apparel, Women's Apparel, Footwear, Accessories
- **vendors-sample.csv** — three textile/garment suppliers (Surat, Tirupur, Ludhiana —
  India's actual textile manufacturing hubs), with realistic GSTINs and state codes
- **customers-sample.csv** — a mix of individual (B2C, no GSTIN) and retailer (B2B,
  with GSTIN) customers
- **products-sample.csv** — the one worth reading closely: **one row per variant**,
  not per product. Rows sharing the same "Product Name" become one product with
  multiple variants (different Size/Color). GST rates follow the real Indian rule for
  apparel: 5% for garments priced ≤ ₹1000, 12% above that — notice the ₹499 t-shirt is
  5% and the ₹1799 jeans / ₹1499 kurta are 12%. Any category name that doesn't already
  exist in your tenant gets created automatically during import.

## Import order

Upload in this order so references resolve correctly:

1. `categories-sample.csv` (optional — products-sample.csv will auto-create any
   missing category anyway, but uploading this first lets you see/edit them before
   products are attached to them)
2. `products-sample.csv`
3. `vendors-sample.csv`
4. `customers-sample.csv`

## Expected result

- 4 categories, 3 products (5 variants total), 3 vendors, 3 customers
- Every row should succeed - if you see import errors, most likely cause is
  re-uploading the same file twice (duplicate category name, SKU, or vendor/customer
  isn't actually rejected on duplicate name for vendors/customers, but categories and
  product SKUs *do* reject duplicates)
