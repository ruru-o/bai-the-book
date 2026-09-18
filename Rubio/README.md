# rubio

task: input validation and network resilience

where to edit your codebase:
- edit files inside your folder: Rubio/resilience/
  1. Rubio/resilience/RubioInputValidator.kt - sanitize note inputs and enforce file size limits
  2. Rubio/resilience/NetworkResilience.kt - implement exponential backoff retries for network drops

how to connect:
- validate upload input in QuickUploadWidget before calling Alvarez's service
