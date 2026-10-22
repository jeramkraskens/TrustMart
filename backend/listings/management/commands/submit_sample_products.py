"""Attach generated sample images and submit only the seeded draft listings."""
import json
from pathlib import Path

from django.core.files import File
from django.core.management.base import BaseCommand, CommandError
from django.db import transaction
from PIL import Image

from listings.models import Listing, ListingImage
from verification.models import VerificationRequest
from .seed_seller_products import PRODUCTS


class Command(BaseCommand):
    help = 'Attach three generated sample photos per seeded product and submit for verification.'

    def add_arguments(self, parser):
        parser.add_argument('manifest', type=Path)

    def handle(self, *args, **options):
        assets = json.loads(options['manifest'].read_text(encoding='utf-8'))
        if len(assets) != 30:
            raise CommandError('Expected exactly 30 sample images.')
        grouped = {}
        for asset in assets:
            path = Path(asset['source'])
            with Image.open(path) as image:
                image.verify()
            grouped.setdefault(asset['id'], []).append(asset)
        if len(grouped) != 10 or any(sorted(a['index'] for a in rows) != [1, 2, 3] for rows in grouped.values()):
            raise CommandError('Each of ten products must have views 1, 2 and 3.')
        titles = {row[1] for row in PRODUCTS}
        # Validate every target before writing files or changing database state.
        with transaction.atomic():
            listings = list(Listing.objects.select_for_update().filter(id__in=grouped, seller__username='seller1', title__in=titles))
            if len(listings) != 10:
                raise CommandError('Sample listing ownership or titles changed; no submission made.')
            for listing in listings:
                if listing.status not in (Listing.Status.DRAFT, Listing.Status.PENDING):
                    raise CommandError(f'Listing {listing.id} is already reviewed; refusing to reset it.')
                expected = {f'listings/sample-{listing.id}-{n}.png' for n in (1, 2, 3)}
                existing = set(listing.images.values_list('image', flat=True))
                if existing - expected:
                    raise CommandError(f'Listing {listing.id} already has other photos; refusing to replace them.')
            for listing in listings:
                for asset in sorted(grouped[listing.id], key=lambda item: item['index']):
                    filename = f"listings/sample-{listing.id}-{asset['index']}.png"
                    photo, _ = ListingImage.objects.get_or_create(listing=listing, image=filename)
                    storage = photo.image.storage
                    if not storage.exists(filename):
                        with Path(asset['source']).open('rb') as source:
                            saved = storage.save(filename, File(source))
                        if saved != filename:
                            photo.image = saved
                            photo.save(update_fields=['image'])
                disclosure = 'Photos are AI-generated samples for demonstration, not photographs of the actual item.'
                if disclosure not in listing.description:
                    listing.description += '\n\n' + disclosure
                VerificationRequest.objects.get_or_create(
                    listing=listing, status=VerificationRequest.Status.PENDING,
                    defaults={'notes': 'Demo product submitted with three illustrative sample photos. ' + disclosure},
                )
                listing.status = Listing.Status.PENDING
                listing.save(update_fields=['status', 'description', 'updated_at'])
                self.stdout.write(f'{listing.id}: {listing.title} | {listing.images.count()} images | {listing.status}')
        self.stdout.write(self.style.SUCCESS('All ten sample products submitted for moderator verification.'))
