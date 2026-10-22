# Django admin registration for product listings, categories, photos and saved items.
# This configures the built-in /admin/ site, separate from the React admin dashboard.
from django.contrib import admin
from .models import Category, Listing, ListingImage, Wishlist


class ListingImageInline(admin.TabularInline):
    model = ListingImage
    extra = 1


@admin.register(Listing)
class ListingAdmin(admin.ModelAdmin):
    list_display = ['title', 'seller', 'category', 'price', 'status', 'created_at']
    list_filter = ['status', 'category']
    inlines = [ListingImageInline]


admin.site.register(Category)
admin.site.register(Wishlist)
