package ncei.gis

interface ProcessingOperations {
    File createGrid(List coords, String survey, File surveyManifestFile)
    File createLERC(File gridFile)
    void initSurvey(String survey)
    void cleanupSurvey(String survey)
}
