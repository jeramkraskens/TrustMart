from django.core.management.base import BaseCommand
from accounts.models import User


class Command(BaseCommand):
    help = "Fix existing superuser accounts whose role field was never set to 'admin' (e.g. accounts created before this fix)."

    def handle(self, *args, **options):
        fixed = 0
        for u in User.objects.filter(is_superuser=True).exclude(role='admin'):
            u.role = 'admin'
            u.save(update_fields=['role'])
            self.stdout.write(self.style.SUCCESS(f"Fixed: {u.username} -> role=admin"))
            fixed += 1

        if fixed == 0:
            self.stdout.write(self.style.SUCCESS('No accounts needed fixing. All superusers already have role=admin.'))
        else:
            self.stdout.write(self.style.SUCCESS(f'Done. Fixed {fixed} account(s).'))
