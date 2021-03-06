package sample;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;

import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;
import java.util.logging.Logger;

import javafx.collections.ObservableList;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.swing.*;

import static javafx.application.Application.launch;

public class SearchMenuController {

    private String searchText;
    private @FXML CheckBox isWineBox, isBeerBox, isOtherBox;
    private @FXML TextField searchTextField;
    private @FXML TableColumn idColumn, nameColumn, brandNameColumn, alcoholTypeColumn, locationColumn;
    private @FXML TableView table;
    private @FXML RadioButton normalSearch, intersectSearch, unionSearch;
    private @FXML Button helpSearchButton;

    private @FXML RadioButton csvDownload, tabDownload, customDownload;
    private @FXML TextField CustomDelimiter;// customDirectoryField;
    //  private @FXML CheckBox CustomDirectoryCheckBox;
    private @FXML ChoiceBox<String> choiceBox;

    private ScreenUtil screenUtil = new ScreenUtil();
    private int alcoholChoice = 0;
    private final int BEER = 1;
    private final int WINE = 2;
    private final int DISTILLED = 3;

    private List<AlcoholData> alcoholDataList = new ArrayList<AlcoholData>();
    private ObservableList<AlcoholData> observableList;

    private DatabaseUtil dbUtil = new DatabaseUtil();
    private String choiceSearch;

    @FXML
    public void initialize(){
        // to get information (alcohol data) from the double clicked row in the table
        table.setRowFactory(tv -> {
            TableRow<AlcoholData> row = new TableRow<AlcoholData>();
            row.setOnMouseClicked(event -> {
                if(event.getClickCount() == 2 && (! row.isEmpty())){
                    AlcoholData rowData = row.getItem();
                    screenUtil.pullUpAlcoholDetails(rowData);
                }
            });
            row.setTooltip(new Tooltip("Double click to see more detail"));
            return row;
        });
        //adds options
        choiceBox.getItems().addAll("All Fields", "ID", "Name", "Brand Name", "Location", "Alcohol Content");
        //sets default vaule
        choiceBox.setValue("All Fields");
    }


    public void displayResults(){
        table.getColumns().clear();
        idColumn.setCellValueFactory(new PropertyValueFactory<>("aid"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("Name"));
        brandNameColumn.setCellValueFactory(new PropertyValueFactory<>("BrandName"));
        alcoholTypeColumn.setCellValueFactory(new PropertyValueFactory<>("AlcoholType"));
        locationColumn.setCellValueFactory(new PropertyValueFactory<>("Appellation"));
        table.setItems(this.getObservableList());
        table.getColumns().addAll(idColumn, nameColumn, brandNameColumn, alcoholTypeColumn, locationColumn);
    }

    public void back (ActionEvent event){
        screenUtil.switchScene("MainMenu.fxml", "Main Menu");
    }

    public ObservableList<AlcoholData> getObservableList() {
        return observableList;
    }

    public void search(ActionEvent event) throws SQLException, NoSuchMethodException, IllegalAccessException, InstantiationException, IOException{
        if(normalSearch.isSelected()){
            searchDatabase();
        }
        else if(intersectSearch.isSelected()){
            searchIntersect();
        }
        else {
            searchUnion();
        }
        observableList = FXCollections.observableList(alcoholDataList);
        displayResults();
        System.out.println("Search finished");
    }


    //"All Fields", "ID", "Name", "Brand Name", "Location", "Alcohol Content"
    //TODO add other spirits
    private void searchDatabase() throws SQLException {
        if (isWineBox.isSelected() && isBeerBox.isSelected() && isOtherBox.isSelected()){
            alcoholDataList = dbUtil.searchAlcoholWithType(BEER);
            alcoholDataList.addAll(dbUtil.searchAlcoholWithType(WINE));
            alcoholDataList.addAll(dbUtil.searchAlcoholWithType(DISTILLED));
            alcoholDataList = interesectAlcoholData(searchByChoice(), alcoholDataList);
        }
        else if (isWineBox.isSelected() && isBeerBox.isSelected()){
            alcoholDataList = dbUtil.searchAlcoholWithType(BEER);
            alcoholDataList.addAll(dbUtil.searchAlcoholWithType(WINE));
            alcoholDataList = interesectAlcoholData(searchByChoice(), alcoholDataList);
        }
        else if (isOtherBox.isSelected() && isBeerBox.isSelected()) {
            alcoholDataList = dbUtil.searchAlcoholWithType(BEER);
            alcoholDataList.addAll(dbUtil.searchAlcoholWithType(DISTILLED));
            alcoholDataList = interesectAlcoholData(searchByChoice(), alcoholDataList);
        }
        else if (isOtherBox.isSelected() && isWineBox.isSelected()) {
            alcoholDataList = dbUtil.searchAlcoholWithType(WINE);
            alcoholDataList.addAll(dbUtil.searchAlcoholWithType(DISTILLED));
            alcoholDataList = interesectAlcoholData(searchByChoice(), alcoholDataList);
        }
        else if(isWineBox.isSelected() || isBeerBox.isSelected() || isOtherBox.isSelected()){
            if (isBeerBox.isSelected()){
                alcoholChoice = 1;
            }
            else if (isWineBox.isSelected()){
                alcoholChoice = 2;
            }
            else if (isOtherBox.isSelected()){
                alcoholChoice = 3;
            }
            alcoholDataList = dbUtil.searchAlcoholWithType(alcoholChoice);
            alcoholDataList = interesectAlcoholData(searchByChoice(), alcoholDataList);
        }
        else {
            alcoholDataList = searchByChoice();
        }
    }

    //"All Fields", "ID", "Name", "Brand Name", "Location", "Alcohol Content"
    public List<AlcoholData> searchByChoice() throws SQLException {
        choiceSearch = choiceBox.getValue();
        List<AlcoholData> adl = new ArrayList<>();
        searchText = searchTextField.getText();
        if(searchText.trim().isEmpty()){
            adl = dbUtil.getAllAlcoholEntries();
        }else{
            if (choiceSearch.equals("ID")) {
                int searchIntegerValue = Integer.parseInt(searchText);
                adl = dbUtil.searchAlcoholID(searchIntegerValue);
            } else if (choiceSearch.equals("Name")) {
                adl = dbUtil.searchAlcoholName(searchText);
            } else if (choiceSearch.equals("Brand Name")) {
                adl = dbUtil.searchAlcoholBrand(searchText);
            } else if (choiceSearch.equals("Location Name")) {
                adl = dbUtil.searchAlcoholAppellation(searchText);
            } else if (choiceSearch.equals("Alcohol Content")) {
                double searchDoubleValue = Double.parseDouble(searchText);
                adl = dbUtil.searchAlcoholContent(searchDoubleValue);
            } else {
                adl = searchAllAlcoholFields(searchText);
            }

        }
        //fills a new list of alcohol data with elements that only match the search

        //checks to see if the user wants beer or wine
        //

        //adl becomes the overlapping elements of both lists
       /* for(int i = adl.size() - 1; i > -1; --i) {
            AlcoholData alcDat = adl.get(i);
            if(!alcoholDataList.remove(alcDat))
                adl.remove(alcDat);
        }*/

        //sets alcohol data list to the correct elements
        return adl;
    }

    public void searchUnion() throws SQLException
    {
        List<AlcoholData> previousAlcoholDataResults = alcoholDataList;
        searchDatabase();

        alcoholDataList = mergeAlcoholData(previousAlcoholDataResults, alcoholDataList);

        /*List<AlcoholData> alcoholDataListTemp = new ArrayList<AlcoholData>();
        for(int i = 0; i< alcoholDataList.size(); i++){
            alcoholDataListTemp.add(alcoholDataList.get(i));
        }
        alcoholDataList.clear();
        if (isBeerBox.isSelected()){
            alcoholChoice = 1;
        }
        else if (isWineBox.isSelected()){
            alcoholChoice = 2;
        }
        else if (searchTextField.getText() == null || searchTextField.getText().trim().isEmpty()) {
            searchText = "";
            //System.out.println("BRAND NAME EMPTY");
           // screenUtil.switchScene("ErrorState.fxml","Error");
           // System.out.println("CHOOSE ALCOHOL TYPE OR BRANDNAME");
        }
        else
            searchText = searchTextField.getText();
        searchByChoice();
        boolean to_add;
        int temp_int = alcoholDataListTemp.size();
        System.out.println((temp_int));
        for(int i=0; i < alcoholDataListTemp.size(); i++) {
            to_add = true;
            System.out.println("Checking if item is in list");
            for (int j = 0; j < alcoholDataList.size(); j++) {
                if (alcoholDataList.get(j).myEquals(alcoholDataListTemp.get(i))) {
                    to_add = false;
                    break;
                }
            }
            if(to_add){
                alcoholDataList.add(alcoholDataListTemp.get(i));
            }
        }*/

    }

    public void searchIntersect() throws SQLException
    {

        List<AlcoholData> previousAlcoholDataResults = alcoholDataList;
        searchDatabase();

        alcoholDataList = interesectAlcoholData(previousAlcoholDataResults, alcoholDataList);

       /* List<AlcoholData> alcoholDataListTemp = new ArrayList<AlcoholData>();
        for(int i = 0; i< alcoholDataList.size(); i++){
            alcoholDataListTemp.add(alcoholDataList.get(i));
        }
        alcoholDataList.clear();
        if (isBeerBox.isSelected()){
            isWineBox.setSelected(false);
            alcoholChoice = 1;
        }
        else if (isWineBox.isSelected()){
            alcoholChoice = 2;
        }else if (searchTextField.getText() == null || searchTextField.getText().trim().isEmpty()) {
            searchText = " ";
           // System.out.println("BRAND NAME EMPTY");
           // screenUtil.switchScene("ErrorState.fxml","Error");
           // System.out.println("CHOOSE ALCOHOL TYPE OR searchText");
        }
        else
            searchText = searchTextField.getText();
        searchByChoice();
        boolean to_remove;
        for(int i=0; i < alcoholDataList.size(); i++) {
            to_remove = true;
            for (int j = 0; j < alcoholDataListTemp.size(); j++) {
                if (alcoholDataListTemp.get(j).myEquals(alcoholDataList.get(i))) {
                    to_remove = false;
                    break;
                }
            }
            if(to_remove) {
                alcoholDataList.remove(alcoholDataList.get(i));
                i--;
            }
        }*/
    }

    public List<AlcoholData> searchAllAlcoholFields(String searchText) throws SQLException{
        List<AlcoholData> finalResultList = new ArrayList<AlcoholData>();

        try{
            int searchAlcTypeInput = Integer.parseInt(searchText);
            List<AlcoholData> alcoholTypeResults = dbUtil.searchAlcoholWithType(searchAlcTypeInput);
            finalResultList = mergeAlcoholData(alcoholTypeResults, finalResultList);
        }catch (Exception e){
            System.out.println("Could not search type field using input");
        }
        List<AlcoholData> alcoholBrandResults = dbUtil.searchAlcoholBrand(searchText);
        finalResultList = mergeAlcoholData(alcoholBrandResults, finalResultList);
        try{
            int searchIDInput = Integer.parseInt(searchText);
            List<AlcoholData> alcoholIDResults = dbUtil.searchAlcoholID(searchIDInput);
            finalResultList = mergeAlcoholData(alcoholIDResults, finalResultList);
        }catch (Exception e){
            System.out.println("Could not search id field using input");
        }
        List<AlcoholData> alcoholNameResults = dbUtil.searchAlcoholName(searchText);
        finalResultList = mergeAlcoholData(alcoholNameResults, finalResultList);
        List<AlcoholData> alcoholAppelationResults = dbUtil.searchAlcoholAppellation(searchText);
        finalResultList = mergeAlcoholData(alcoholAppelationResults, finalResultList);
        try {
            double searchAlcContInput = Double.parseDouble(searchText);
            List<AlcoholData> alcoholContentResults = dbUtil.searchAlcoholContent(searchAlcContInput);
            finalResultList = mergeAlcoholData(alcoholContentResults, finalResultList);
        }catch (Exception e){
            System.out.println("Could not search alcohol content field using input");
        }

        return finalResultList;

    }

    public List<AlcoholData> mergeAlcoholData(List<AlcoholData> listToAdd, List<AlcoholData> originalList){
        boolean toAdd;

        if(originalList.size() == 0){
            return listToAdd;
        }

        for(int i=0; i < listToAdd.size(); i++) {
            toAdd = true;

            for(int j=0; j < originalList.size(); j++) {
                if(originalList.get(j).getAid() == listToAdd.get(i).getAid()){
                    toAdd = false;
                    break;
                }
            }

            if(toAdd) {
                originalList.add(listToAdd.get(i));
            }
        }

        return originalList;
    }

    public List<AlcoholData> interesectAlcoholData(List<AlcoholData> listToAdd, List<AlcoholData> originalList){
        ArrayList<AlcoholData> intersectedList = new ArrayList<AlcoholData>();
        boolean toAdd;

        if(originalList.size() == 0){
            return listToAdd;
        }

        for(int i=0; i < listToAdd.size(); i++) {
            toAdd = false;

            for(int j=0; j < originalList.size(); j++) {
                if(originalList.get(j).getAid() == listToAdd.get(i).getAid()){
                    toAdd = true;
                    break;
                }
            }

            if(toAdd) {
                intersectedList.add(listToAdd.get(i));
            }
        }

        return intersectedList;
    }

    /* we do not really need this anymore since we have search by choice
    public void searchNormal() throws SQLException
    {
        alcoholDataList.clear();
        if (isBeerBox.isSelected()){
            alcoholChoice = 1;
        }
        else if (isWineBox.isSelected()){
            alcoholChoice = 2;
        }
        else if (searchTextField.getText() == null || searchTextField.getText().trim().isEmpty()) {
            brandName = " ";
        //    System.out.println("BRAND NAME EMPTY");
        //    screenUtil.switchScene("ErrorState.fxml","Error");
         //   System.out.println("CHOOSE ALCOHOL TYPE OR BRANDNAME");
        }
        else
            brandName = searchTextField.getText();
        searchDatabase();
    }
    */

    public void displayAll(ActionEvent event) throws SQLException, NoSuchMethodException, IllegalAccessException, InstantiationException, IOException{
        alcoholDataList.clear();
        alcoholDataList = dbUtil.searchAlcoholWithType(BEER);
        alcoholDataList.addAll(dbUtil.searchAlcoholWithType(WINE));
        alcoholDataList.addAll(dbUtil.searchAlcoholWithType(DISTILLED));
        observableList = FXCollections.observableList(alcoholDataList);
        displayResults();
    }

    private static final String TAB_DELIMITER = "   ";
    private static final String COMMA_DELIMITER = ",";
    private String CUSTOM_DELIMITER = "";
    private String DELIMITER = "";
    private String fileType = "";

    private static final String NEW_LINE_SEPARATOR = "\r\n";
    //private String fileName = "CS3733_TeamA";
    private String fileName = /*System.getProperty("user.home") + "/"+*/"CS3733_TeamA";// + ".csv";
    private File F = new File(fileName);
    private String newFileNumber = "0";
    private String fileContents = "";
    private String customDirectoryString = "";
    private String fileDirectoryWithName = "";


    //public void updateCustomDir(){
    // customDirectoryString = customDirectoryField.getText();
    ///     System.out.println(customDirectoryString);
    //     System.out.println("dasdfjjsafs");
    //  }

//    public void download(){
//        if(CustomDirectoryCheckBox.isSelected())
//        {
//           // fileName = System.getProperty("user.home") + "/"+"CS3733_TeamA" + ".csv";
//           // customDirectoryString = System.getProperty("user.home") + "/";
//            customDirectoryString = customDirectoryField.getText();
//            System.out.println(customDirectoryString);
//            if(!customDirectoryString.substring(customDirectoryString.length()-1,customDirectoryString.length()-1).equals("/"))
//            {
//              ///  customDirectoryString = customDirectoryString + "/";
//            }
//        }else{
//
//            customDirectoryString = System.getProperty("user.home") + "/";
//        }
//
//        if(csvDownload.isSelected()){
//            System.out.println("comma delim");
//            DELIMITER = COMMA_DELIMITER;
//            fileType = ".csv";
//        }else if (tabDownload.isSelected()){
//            System.out.println("tab delim");
//            DELIMITER = TAB_DELIMITER;
//            fileType = ".txt";
//        }else if (customDownload.isSelected()){
//            System.out.println("custom");
//            CUSTOM_DELIMITER = customDownload.getText();
//            System.out.println(CUSTOM_DELIMITER);
//
//            DELIMITER = CUSTOM_DELIMITER;
//            fileType = ".txt";
//        }
//
//
//        if(F.exists()) {
//           // for (j = 1; F.exists(); j++) {
//                int fileDirectoryWithNameLength = fileDirectoryWithName.length();
//                fileDirectoryWithName =  /*customDirectoryString +*/ fileDirectoryWithName.substring(0,fileDirectoryWithNameLength-4) + /*String.valueOf(j)*/ newFileNumber + fileType;
//                newFileNumber = Integer.toString(Integer.parseInt(newFileNumber)+1);
//            //}
//        }else{
//            fileDirectoryWithName =  customDirectoryString + fileName + fileType;
//        }
//        System.out.println("setting file dir with name");
//        //fileDirectoryWithName =  customDirectoryString + fileName + fileType;
//        F = new File(fileDirectoryWithName);
//        FileWriter fileWriter = null;
//        System.out.println("fileWriter set to null");
//        try {
//            fileWriter = new FileWriter(fileDirectoryWithName);
//            System.out.println("writing file contents in string");
//            //Write the CSV file header
//            fileContents = fileContents +"ID";
//            fileContents = fileContents + (DELIMITER);
//            fileContents = fileContents + ("Name");
//            fileContents = fileContents + (DELIMITER);
//            fileContents = fileContents + ("Brandname");
//            fileContents = fileContents + (DELIMITER);
//            fileContents = fileContents + ("App");
//            fileContents = fileContents + (DELIMITER);
//            fileContents = fileContents + ("Type");
//            fileContents = fileContents + (DELIMITER);
//
//            //AlcoholData(ID, name, brandname, app, type)
//            for (int i = 0; i< alcoholDataList.size(); i++) {
//                fileContents = fileContents + (String.valueOf(alcoholDataList.get(i).getAid()));
//                fileContents = fileContents + (DELIMITER);
//                fileContents = fileContents + (alcoholDataList.get(i).getName());
//                fileContents = fileContents + (DELIMITER);
//                fileContents = fileContents + (alcoholDataList.get(i).getBrandName());
//                fileContents = fileContents + (DELIMITER);
//                fileContents = fileContents + (alcoholDataList.get(i).getAppellation());
//                fileContents = fileContents + (DELIMITER);
//                fileContents = fileContents + (String.valueOf(alcoholDataList.get(i).getAlcoholType()));
//                //fileWriter.append(data[i].toString());
//                //fileWriter.append(COMMA_DELIMITER);
//                fileContents = fileContents + (NEW_LINE_SEPARATOR);
//            }
//            fileWriter.append(fileContents);
//            System.out.println("CSV file was created successfully !!!");
//            System.out.println("CSV file name:"+fileDirectoryWithName);
//
//        } catch (Exception e) {
//            System.out.println("Error in CsvFileWriter !!!");
//            e.printStackTrace();
//        } finally {
//
//            try {
//                fileWriter.flush();
//                fileWriter.close();
//            } catch (IOException e) {
//                System.out.println("Error while flushing/closing fileWriter !!!");
//                e.printStackTrace();
//            }
//
//        }
//    }//

    ////////////////////////////////////////////////////////////////////
    public void download2(){
        if(csvDownload.isSelected()){
            System.out.println("comma delim");
            DELIMITER = COMMA_DELIMITER;
            fileType = ".csv";
        }else if (tabDownload.isSelected()){
            System.out.println("tab delim");
            DELIMITER = TAB_DELIMITER;
            fileType = ".txt";
        }else if (customDownload.isSelected()){
            System.out.println("custom");
            CUSTOM_DELIMITER = CustomDelimiter.getText();
            System.out.println(CUSTOM_DELIMITER);

            DELIMITER = CUSTOM_DELIMITER;
            fileType = ".txt";
        }
        fileContents = "";
        fileContents = fileContents +"ID";
        fileContents = fileContents + (DELIMITER);
        fileContents = fileContents + ("Name");
        fileContents = fileContents + (DELIMITER);
        fileContents = fileContents + ("Brandname");
        fileContents = fileContents + (DELIMITER);
        fileContents = fileContents + ("App");
        fileContents = fileContents + (DELIMITER);
        fileContents = fileContents + ("Type");
        fileContents = fileContents + (NEW_LINE_SEPARATOR);

        //AlcoholData(ID, name, brandname, app, type)
        for (int i = 0; i< alcoholDataList.size(); i++) {
            fileContents = fileContents + (String.valueOf(alcoholDataList.get(i).getAid()));
            fileContents = fileContents + (DELIMITER);
            fileContents = fileContents + (alcoholDataList.get(i).getName());
            fileContents = fileContents + (DELIMITER);
            fileContents = fileContents + (alcoholDataList.get(i).getBrandName());
            fileContents = fileContents + (DELIMITER);
            fileContents = fileContents + (alcoholDataList.get(i).getAppellation());
            fileContents = fileContents + (DELIMITER);
            fileContents = fileContents + (String.valueOf(alcoholDataList.get(i).getAlcoholType()));
            //fileWriter.append(data[i].toString());
            //fileWriter.append(COMMA_DELIMITER);
            fileContents = fileContents + (NEW_LINE_SEPARATOR);
        }
        //new Stage() = savePopUp;
        start(new Stage());
    }


    //////////////////////////////////////////////////////////////////////////
    //@Override
    public void start(Stage primaryStage) {
        //primaryStage.setTitle("java-buddy.blogspot.com");
        // Group root = new Group();

        // TextArea textArea = new TextArea();

        // Button buttonSave = new Button("Save");

        //  buttonSave.setOnAction((ActionEvent event) -> {
        FileChooser fileChooser = new FileChooser();

        //Set extension filter
        FileChooser.ExtensionFilter extFilter =
                new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
        new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv");
        fileChooser.getExtensionFilters().add(extFilter);

        //Show save file dialog
        File file = fileChooser.showSaveDialog(primaryStage);

        if(file != null){
            SaveFile(fileContents, file);
        }
    }//);

    // VBox vBox = new VBox();
    //vBox.getChildren().addAll(textArea, buttonSave);

    // root.getChildren().add(vBox);

    // primaryStage.setScene(new Scene(root, 500, 400));
    //primaryStage.show();
    //}

    public static void main(String[] args) {
        launch(args);
    }

    private void SaveFile(String content, File file){
        try {
            FileWriter fileWriter;

            fileWriter = new FileWriter(file);
            fileWriter.write(content);
            fileWriter.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.out.println("IOException");
            //Logger.getLogger(JavaFXSaveText.class
            //        .getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void buttonClicked (){
        screenUtil.switchScene("SearchHelp.fxml","Help");
    }

}

