from django.contrib import admin
from django.contrib.auth.admin import UserAdmin
from .models import User, SiteSettings


@admin.register(User)
class CustomUserAdmin(UserAdmin):
    list_display = ['username', 'email', 'role', 'is_staff', 'date_joined']
    list_filter = ['role', 'is_staff', 'is_active']
    fieldsets = UserAdmin.fieldsets + (
        ('额外信息', {'fields': ('role', 'avatar', 'phone')}),
    )
    add_fieldsets = UserAdmin.add_fieldsets + (
        ('额外信息', {'fields': ('role', 'avatar', 'phone')}),
    )


@admin.register(SiteSettings)
class SiteSettingsAdmin(admin.ModelAdmin):
    list_display = ['site_name', 'primary_color', 'secondary_color', 'updated_at']
    readonly_fields = ['created_at', 'updated_at']


