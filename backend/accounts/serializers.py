# API data conversion for user accounts, roles and profile details.
# Serializers turn model records into response data and validate incoming fields before saving.
from django.contrib.auth import get_user_model
from django.contrib.auth.password_validation import validate_password
from rest_framework import serializers
from rest_framework_simplejwt.serializers import TokenObtainPairSerializer
from listings.locations import LOCATIONS
from .validators import validate_phone

User = get_user_model()


# Section: Account validation — required details, district and seller NIC.
class AccountDetailsSerializer(serializers.ModelSerializer):
    first_name = serializers.CharField(max_length=150)
    last_name = serializers.CharField(max_length=150)
    email = serializers.EmailField()
    phone = serializers.CharField(validators=[validate_phone], trim_whitespace=False)
    district = serializers.ChoiceField(choices=list(LOCATIONS))
    # Section: NIC format — accept any digit count and an optional V/X suffix.
    nic_number = serializers.RegexField(r'\A[0-9]+\Z', required=False, trim_whitespace=False,
        error_messages={'invalid': 'Enter digits only for your NIC number.'})
    nic_suffix = serializers.ChoiceField(choices=['V', 'X'], required=False, allow_blank=True)
    def validate(self, attrs):
        attrs = super().validate(attrs)
        role = attrs.get('role', getattr(self.instance, 'role', 'buyer'))
        # Section: Seller identity — require the NIC number, but allow a blank or omitted letter.
        if role == 'seller' and (not self.instance or any(key in attrs for key in ('first_name', 'last_name', 'email', 'phone', 'district', 'nic_number', 'nic_suffix'))):
            if not attrs.get('nic_number', getattr(self.instance, 'nic_number', '')):
                raise serializers.ValidationError({'nic_number': 'This field is required for sellers.'})
        elif role != 'seller' and any(key in attrs for key in ('nic_number', 'nic_suffix')):
            raise serializers.ValidationError({'nic_number': 'NIC details are only accepted for seller accounts.'})
        return attrs


class UserSerializer(AccountDetailsSerializer):
    class Meta:
        model = User
        fields = ['id', 'username', 'email', 'first_name', 'last_name',
                  'role', 'phone', 'district', 'nic_number', 'nic_suffix', 'trust_score', 'is_banned', 'created_at']
        read_only_fields = ['id', 'role', 'trust_score', 'is_banned', 'created_at']


class AdminUserSerializer(serializers.ModelSerializer):
    """Used only by admin-facing endpoints — allows editing role and ban status."""
    listings_count = serializers.IntegerField(source='listings.count', read_only=True)
    purchases_count = serializers.IntegerField(source='purchases.count', read_only=True)

    def validate_is_banned(self, value):
        if value and self.instance == self.context['request'].user:
            raise serializers.ValidationError('You cannot ban your own administrator account.')
        return value

    class Meta:
        model = User
        fields = ['id', 'username', 'email', 'first_name', 'last_name', 'role', 'phone',
                  'trust_score', 'is_banned', 'is_staff', 'created_at', 'listings_count', 'purchases_count']
        read_only_fields = ['id', 'created_at', 'is_staff', 'listings_count', 'purchases_count']


# Section: Password confirmation — compare both entries before hashing the password.
class ConfirmedPasswordSerializer(serializers.ModelSerializer):
    password = serializers.CharField(write_only=True, trim_whitespace=False, validators=[validate_password])
    password_confirm = serializers.CharField(write_only=True, trim_whitespace=False,
        error_messages={'required': 'Please re-enter your password.', 'blank': 'Please re-enter your password.'})

    def validate(self, attrs):
        if attrs['password'] != attrs['password_confirm']:
            raise serializers.ValidationError({'password_confirm': 'Passwords do not match.'})
        return attrs

    def create(self, validated_data):
        validated_data.pop('password_confirm')
        return User.objects.create_user(**validated_data)


class AdminCreateUserSerializer(AccountDetailsSerializer, ConfirmedPasswordSerializer):
    """Admin-only: create a user account directly with any role (including moderator/admin),
    unlike public registration which is restricted to buyer/seller."""
    role = serializers.ChoiceField(choices=User.Role.choices)

    class Meta:
        model = User
        fields = ['id', 'username', 'email', 'password', 'password_confirm', 'first_name', 'last_name', 'role', 'phone', 'district', 'nic_number', 'nic_suffix']


class RegisterSerializer(AccountDetailsSerializer, ConfirmedPasswordSerializer):
    role = serializers.ChoiceField(choices=[User.Role.BUYER, User.Role.SELLER])

    class Meta:
        model = User
        fields = ['username', 'email', 'password', 'password_confirm', 'first_name', 'last_name', 'role', 'phone',
                  'district', 'nic_number', 'nic_suffix']


class MyTokenObtainPairSerializer(TokenObtainPairSerializer):
    def validate(self, attrs):
        data = super().validate(attrs)
        data['user'] = UserSerializer(self.user).data
        return data
