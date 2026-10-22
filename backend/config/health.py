from django.http import JsonResponse


def health(request):
    """Lightweight process health check without exposing configuration."""
    return JsonResponse({'status': 'ok'})
