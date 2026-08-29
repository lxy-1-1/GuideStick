import json
import os
import subprocess
import sys
from pathlib import Path

from django.test import SimpleTestCase


PROJECT_ROOT = Path(__file__).resolve().parent.parent


class EnvironmentSettingsTests(SimpleTestCase):
    def load_settings(self, **overrides):
        environment = os.environ.copy()
        environment.update(overrides)
        script = """
import json
from GuideSticks import settings

print(json.dumps({
    'secret_key': settings.SECRET_KEY,
    'debug': settings.DEBUG,
    'allowed_hosts': settings.ALLOWED_HOSTS,
    'database': settings.DATABASES['default'],
}, default=str))
"""
        completed = subprocess.run(
            [sys.executable, "-c", script],
            cwd=PROJECT_ROOT,
            env=environment,
            check=True,
            capture_output=True,
            text=True,
        )
        return json.loads(completed.stdout)

    def test_environment_overrides_security_settings(self):
        settings = self.load_settings(
            DJANGO_SECRET_KEY="test-secret-key",
            DJANGO_DEBUG="false",
            DJANGO_ALLOWED_HOSTS="api.example.com, localhost",
        )

        self.assertEqual(settings["secret_key"], "test-secret-key")
        self.assertFalse(settings["debug"])
        self.assertEqual(settings["allowed_hosts"], ["api.example.com", "localhost"])

    def test_sqlite_can_be_enabled_for_local_development(self):
        settings = self.load_settings(DJANGO_USE_SQLITE="true")

        self.assertEqual(settings["database"]["ENGINE"], "django.db.backends.sqlite3")

    def test_environment_overrides_mysql_connection_settings(self):
        settings = self.load_settings(
            DJANGO_DB_ENGINE="django.db.backends.mysql",
            DJANGO_DB_NAME="test_database",
            DJANGO_DB_USER="test_user",
            DJANGO_DB_PASSWORD="test_password",
            DJANGO_DB_HOST="db.example.com",
            DJANGO_DB_PORT="3307",
        )

        self.assertEqual(
            settings["database"],
            {
                "ENGINE": "django.db.backends.mysql",
                "NAME": "test_database",
                "USER": "test_user",
                "PASSWORD": "test_password",
                "HOST": "db.example.com",
                "PORT": "3307",
            },
        )
