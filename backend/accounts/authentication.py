# Checks whether token credentials belong to an account that may still sign in.
# Authentication identifies the user; permissions then decide which actions that user may perform.
from rest_framework.exceptions import AuthenticationFailed
from rest_framework_simplejwt.authentication import JWTAuthentication


# Section: Login eligibility — reject inactive or banned accounts.
def user_can_authenticate(user):
    return bool(user and user.is_active and not user.is_banned)


# Section: Active sessions — block banned users even with an existing token.
class MarketplaceJWTAuthentication(JWTAuthentication):
    def get_user(self, validated_token):
        user = super().get_user(validated_token)
        if user.is_banned:
            raise AuthenticationFailed('Your account is banned. Please contact support.', code='user_banned')
        return user
