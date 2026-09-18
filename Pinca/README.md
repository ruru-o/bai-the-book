# pinca

task: handle data flow pipeline between apis and show results

where to edit your codebase:
- edit files inside your folder: Pinca/pipeline/
  1. Pinca/pipeline/PincaDataPipelineService.kt - bridge Alvarez docling text output into Fernandez gemini prompt input
  2. Pinca/pipeline/ResultsPresenter.kt - parse gemini payload into reviewer domain models for Garcia's ui

how to connect:
- receive extracted text from Alvarez and pass generated models to Garcia's view model
