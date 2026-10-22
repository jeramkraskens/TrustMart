# HTTP request handlers for user accounts, roles and profile details.
# Read the permission settings, then the query filters, then the action methods to follow a request.
from django.contrib.auth import get_user_model
from django.contrib.auth.password_validation import validate_password
from django.core.exceptions import ValidationError
from django_filters.rest_framework import DjangoFilterBackend
from rest_framework import filters, generics, permissions, status, viewsets
from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework_simplejwt.views import TokenObtainPairView
from django.db import transaction
from notifications.utils import notify, notify_staff

from .permissions import IsAdmin
from .serializers import (
    RegisterSerializer, UserSerializer, AdminUserSerializer,
    AdminCreateUserSerializer, MyTokenObtainPairSerializer,
)

User = get_user_model()


# Section: Registration — public buyer and seller account creation.
class RegisterView(generics.CreateAPIView):
    queryset = User.objects.all()
    serializer_class = RegisterSerializer
    permission_classes = [permissions.AllowAny]

    @transaction.atomic
    def perform_create(self, serializer):
        user = serializer.save()
        notify(user, 'account_created', f'Welcome to TrustMart, {user.username}. Your {user.role} account is ready.', link='/profile')
        notify_staff('user_registered', f'New {user.role} account: {user.username}.', roles=('admin',), link='/admin/users')


class MyTokenObtainPairView(TokenObtainPairView):
    serializer_class = MyTokenObtainPairSerializer
    permission_classes = [permissions.AllowAny]


# Section: Profile API — read and update the signed-in user's details.
class MeView(APIView):
    permission_classes = [permissions.IsAuthenticated]

    def get(self, request):
        return Response(UserSerializer(request.user).data)

    def patch(self, request):
        serializer = UserSerializer(request.user, data=request.data, partial=True)
        serializer.is_valid(raise_exception=True)
        serializer.save()
        return Response(serializer.data)


# Section: Password change — verify the current password and validate the replacement.
class ChangePasswordView(APIView):
    permission_classes = [permissions.IsAuthenticated]

    @transaction.atomic
    def post(self, request):
        old_password = request.data.get('old_password', '')
        new_password = request.data.get('new_password', '')

        if not isinstance(new_password, str) or not new_password or new_password != request.data.get('password_confirm'):
            return Response({'password_confirm': 'Please enter matching new passwords.'}, status=status.HTTP_400_BAD_REQUEST)

        if not request.user.check_password(old_password):
            return Response({'old_password': 'Current password is incorrect.'}, status=status.HTTP_400_BAD_REQUEST)

        try:
            validate_password(new_password, user=request.user)
        except ValidationError as e:
            return Response({'new_password': list(e.messages)}, status=status.HTTP_400_BAD_REQUEST)

        request.user.set_password(new_password)
        request.user.save()
        notify(request.user, 'password_changed', 'Your TrustMart password was changed.', link='/profile')
        return Response({'status': 'Password changed successfully.'})


class UserManagementViewSet(viewsets.ModelViewSet):
    """Admin-only: manage all user accounts (create, ban/unban, role changes, list, view, delete)."""
    queryset = User.objects.all().order_by('-created_at')
    serializer_class = AdminUserSerializer
    permission_classes = [IsAdmin]
    filter_backends = [DjangoFilterBackend, filters.SearchFilter]
    filterset_fields = ['role', 'is_banned']
    search_fields = ['username', 'email', 'first_name', 'last_name']

    @transaction.atomic
    def perform_create(self, serializer):
        user = serializer.save()
        notify(user, 'account_created', f'An administrator created your {user.role} account.', link='/profile')
        notify_staff('user_registered', f'{self.request.user.username} created {user.role} account {user.username}.',
                     roles=('admin',), exclude=self.request.user.pk, link='/admin/users')

    @transaction.atomic
    def perform_update(self, serializer):
        before = serializer.instance
        role, banned = before.role, before.is_banned
        user = serializer.save()
        changes = []
        if role != user.role:
            changes.append(f'role changed to {user.role}')
        if banned != user.is_banned:
            changes.append('account banned' if user.is_banned else 'account access restored')
        if changes:
            description = '; '.join(changes)
            notify(user, 'account_updated', f'Administrator update: {description}.', link='/profile')
            notify_staff('account_updated', f'{self.request.user.username} updated {user.username}: {description}.',
                         roles=('admin',), exclude=self.request.user.pk, link='/admin/users')

    def get_serializer_class(self):
        if self.action == 'create':
            return AdminCreateUserSerializer
        return AdminUserSerializer
