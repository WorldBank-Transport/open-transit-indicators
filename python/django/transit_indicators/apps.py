from django.apps import AppConfig


class TransitIndicatorsAppConfig(AppConfig):
    name = 'transit_indicators'
    verbose_name = 'Open Transit Indicators'

    def ready(self):
        import signals  # NOQA
