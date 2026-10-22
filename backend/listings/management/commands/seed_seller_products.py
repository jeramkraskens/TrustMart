"""Add a repeatable sample catalogue without modifying existing seller listings."""
from decimal import Decimal

from django.contrib.auth import get_user_model
from django.core.management.base import BaseCommand, CommandError
from django.db import transaction

from listings.models import Category, Listing


# Prices and descriptions are fictional demo data, not market valuations.
PRODUCTS = [
    ('Mobile Phones', 'Samsung Galaxy A54 5G - 128GB', '78000', 'excellent',
     'Sample listing: 128GB storage, 8GB RAM, dual SIM and charger. Light signs of use; screen and cameras work. Demo details must be confirmed before sale.'),
    ('Mobile Phones', 'Apple iPhone 13 - 128GB', '125000', 'good',
     'Sample listing: 128GB storage, unlocked handset and charging cable. Minor scratches on the frame. Confirm battery health and device ownership before sale.'),
    ('Laptops', 'Dell Latitude 5420 - Core i5, 16GB RAM', '115000', 'excellent',
     'Sample listing: Intel Core i5, 16GB RAM, 512GB SSD and 14-inch display. Includes charger. Suitable for study and office work; verify specifications before sale.'),
    ('Laptops', 'Lenovo ThinkPad T480 - Core i5, 8GB RAM', '85000', 'good',
     'Sample listing: Intel Core i5, 8GB RAM, 256GB SSD and 14-inch display. Includes charger; light keyboard wear. Confirm battery condition before sale.'),
    ('Home Appliances', 'Samsung 7kg Front Load Washing Machine', '68000', 'good',
     'Sample listing: 7kg front-loading washing machine with inlet and drain hoses. Minor exterior marks. Buyer should inspect and test before collection.'),
    ('Home Appliances', 'LG 260L Double Door Refrigerator', '95000', 'excellent',
     'Sample listing: 260-litre double-door refrigerator with adjustable shelves. Clean interior and light exterior wear. Confirm cooling performance before sale.'),
    ('Vehicles', 'Honda Dio 2019 Scooter', '580000', 'good',
     'Sample listing: petrol scooter with electric start and under-seat storage. Mileage, registration and ownership documents must be checked before sale. Inspection required.'),
    ('Vehicles', 'Bajaj Pulsar 150 - 2018', '650000', 'good',
     'Sample listing: 150cc motorcycle with manual transmission. Confirm mileage, service history and registration documents. Arrange an inspection before purchase.'),
    ('Furniture', 'Wooden Dining Table with 4 Chairs', '42000', 'good',
     'Sample listing: wooden dining table and four matching chairs. Minor surface marks from normal use. Confirm dimensions and arrange collection before purchase.'),
    ('Furniture', 'Three-Seater Fabric Sofa - Grey', '55000', 'excellent',
     'Sample listing: grey fabric three-seater sofa with removable cushions. Light use. Confirm dimensions and upholstery condition before collection.'),
]


class Command(BaseCommand):
    help = 'Create ten sample draft products for a seller (safe to rerun).'

    def add_arguments(self, parser):
        parser.add_argument('--seller', default='seller1', help='Existing seller username')

    @transaction.atomic
    def handle(self, *args, **options):
        seller = get_user_model().objects.filter(username=options['seller'], role='seller').first()
        if seller is None:
            raise CommandError('The specified seller account does not exist.')
        created = 0
        for category_name, title, price, condition, description in PRODUCTS:
            category, _ = Category.objects.get_or_create(name=category_name)
            # Never overwrite user edits or reset a listing that was already reviewed.
            listing = Listing.objects.filter(seller=seller, title=title).first()
            if listing is None:
                listing = Listing.objects.create(
                    seller=seller, category=category, title=title,
                    price=Decimal(price), condition=condition, description=description,
                    location='Colombo (sample location)', status=Listing.Status.DRAFT,
                )
                created += 1
            self.stdout.write(f'{listing.id}: {listing.title} | LKR {listing.price} | {listing.status}')
        self.stdout.write(self.style.SUCCESS(f'Created {created} products; kept {len(PRODUCTS) - created} existing products unchanged.'))
