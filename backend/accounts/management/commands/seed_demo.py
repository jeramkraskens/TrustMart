from django.core.management.base import BaseCommand
from accounts.models import User
from listings.models import Category


class Command(BaseCommand):
    help = 'Seed the database with demo accounts and categories for testing.'

    def handle(self, *args, **options):
        if not User.objects.filter(username='admin').exists():
            User.objects.create_superuser('admin', 'admin@example.com', 'Admin@123', role='admin')
            self.stdout.write(self.style.SUCCESS('Created admin / Admin@123'))

        if not User.objects.filter(username='moderator1').exists():
            User.objects.create_user('moderator1', 'moderator1@example.com', 'Moderator@123', role='moderator')
            self.stdout.write(self.style.SUCCESS('Created moderator1 / Moderator@123'))

        if not User.objects.filter(username='seller1').exists():
            User.objects.create_user('seller1', 'seller1@example.com', 'Seller@123', role='seller',
                                       first_name='Nimal', last_name='Perera')
            self.stdout.write(self.style.SUCCESS('Created seller1 / Seller@123'))

        if not User.objects.filter(username='buyer1').exists():
            User.objects.create_user('buyer1', 'buyer1@example.com', 'Buyer@123', role='buyer',
                                       first_name='Kamal', last_name='Silva')
            self.stdout.write(self.style.SUCCESS('Created buyer1 / Buyer@123'))

        categories = ['Electronics', 'Mobile Phones', 'Furniture', 'Fashion', 'Vehicles', 'Home Appliances']
        for name in categories:
            Category.objects.get_or_create(name=name)
        self.stdout.write(self.style.SUCCESS(f'Ensured {len(categories)} categories exist.'))

        self.stdout.write(self.style.SUCCESS('Demo data seeded successfully.'))
