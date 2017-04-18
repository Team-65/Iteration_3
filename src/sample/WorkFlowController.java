package sample;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.util.ArrayList;

public class WorkFlowController {

    @FXML private Label numberOfApplicationsLabel;
    private @FXML TableView inboxTable;
    private @FXML TableColumn ttbIDColumn, fancifulNameColumn, brandNameColumn, alcoholTypeColumn, submissionDateColumn;
    private AccountsUtil accountsUtil = new AccountsUtil();
    private ScreenUtil screenUtil = new ScreenUtil();
    private DatabaseUtil databaseUtil = new DatabaseUtil();

    private String username = accountsUtil.getUsername();
    private int numberOfApps;

    private ObservableList<ApplicationData> observableFormsList;

    @FXML
    public void initialize() throws SQLException{
        roundRobin();

        ArrayList<ApplicationData> formsList = databaseUtil.searchFormWithGovId(databaseUtil.getAccountAid(username));

        numberOfApps = formsList.size();
        numberOfApplicationsLabel.setText(String.valueOf(numberOfApps));

        observableFormsList = FXCollections.observableList(formsList);
        displayResults();
    }

    //goes adds new applications to worker's inboxes
    public void roundRobin() throws  SQLException{
        System.out.println("Running roundrobin");
        ArrayList<ApplicationData> unAssignedForms = databaseUtil.searchUnassignedForms();
        System.out.println("Unassigned forms = "+ unAssignedForms.size());
        if(!(unAssignedForms.size() == 0)){
            for(int i = 0; i < unAssignedForms.size(); i++) {;
                    int GOVID = databaseUtil.searchMinWorkLoad();
                    System.out.println("Found govid with min workload = " + GOVID);
                    databaseUtil.assignForm(GOVID, unAssignedForms.get(i));
                    System.out.println("FORM ID "+ unAssignedForms.get(i) + " ASSIGNED");
            }
        }

    }

    public void displayResults(){
        inboxTable.getColumns().clear();
        ttbIDColumn.setCellValueFactory(new PropertyValueFactory<>("Ttbid"));
        fancifulNameColumn.setCellValueFactory(new PropertyValueFactory<>("FancyName"));
        brandNameColumn.setCellValueFactory(new PropertyValueFactory<>("Brand_name"));
        alcoholTypeColumn.setCellValueFactory(new PropertyValueFactory<>("AlcoholType"));
        submissionDateColumn.setCellValueFactory(new PropertyValueFactory<>("Date"));
        inboxTable.setItems(this.getObservableFormsList());
        inboxTable.getColumns().addAll(ttbIDColumn, fancifulNameColumn, brandNameColumn, alcoholTypeColumn, submissionDateColumn);
    }

    /**
     * Checks if the back button has been clicked and returns user to Main Menu.
     * @param event ActionEvent representing a button press.
     */
    public void goBack (ActionEvent event){
        screenUtil.switchScene("MainMenu.fxml","Main Menu");
    }

    /**
     * Switches to the ApplicationReview screen.
     * @param event Button press representing the ViewFirst button.
     */
    public void viewFirstApplicationButton(ActionEvent event){
        if(numberOfApps <= 0){
            screenUtil.switchScene("WorkFlowController.fxml", "Inbox");
            screenUtil.createAlertBox("No applications due","There are no applications assigned to you at the moment.");
        }else{
            screenUtil.switchScene("ApplicationReview.fxml", "Application Review");
        }
    }

    public ObservableList<ApplicationData> getObservableFormsList() {
        return observableFormsList;
    }
}