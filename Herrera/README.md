# herrera

task: test app and handle error cases

where to edit your codebase:
- edit files inside your folder: Herrera/error/
  1. Herrera/error/HerreraErrorHandler.kt - handle no internet, invalid files, and request timeouts
  2. Herrera/error/ErrorUIComponent.kt - display user-friendly error banners and offline indicators

how to connect:
- hook error handler into MainViewModel state and api service calls
