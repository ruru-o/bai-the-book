# alvarez

task: set up docling api connection and file upload

where to edit your codebase:
- edit files inside your folder: Alvarez/docling/
  1. Alvarez/docling/AlvarezDoclingService.kt - implement docling pdf/docx/pptx text extraction
  2. Alvarez/docling/FileUploadHandler.kt - handle file upload and reject .mp4 files

how to connect:
- pass extracted text string to Pinca's data pipeline service
