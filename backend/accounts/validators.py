# Shared phone and password rules used during account validation.
import re
from django.core.exceptions import ValidationError


# Section: Phone rule — accept exactly ten ASCII digits.
def validate_phone(value):
    if not re.fullmatch(r'[0-9]{10}', value):
        raise ValidationError('Enter exactly 10 digits, without +, spaces or other characters.')
    return value


# Section: Password rule — require length, uppercase, lowercase and a number.
class MixedPasswordValidator:
    def validate(self, password, user=None):
        if len(password) < 8 or not all(re.search(pattern, password) for pattern in (r'[A-Z]', r'[a-z]', r'[0-9]')):
            raise ValidationError(self.get_help_text())

    def get_help_text(self):
        return 'Use at least 8 characters, including an uppercase letter, a lowercase letter and a number.'
