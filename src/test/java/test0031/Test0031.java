package application;
//@@nghuiyirebecca A0130876B
import static org.junit.Assert.assertEquals;

import java.io.File;
import org.junit.Test;

import application.controller.DataManager;
import application.controller.LogicController;
import application.controller.parser.ParserFacade;
import application.exception.InvalidCommandException;
import application.model.LocalStorage;
import application.utils.TasksFormatter;

public class LogicControllerTest {
	public Task(String instruction, Date from, Date deadLine, String ProjectName) {
		this.taskDescription = instruction;
		this.from = from;
		this.deadLine = deadLine;
		this.projectName = ProjectName;

		months = new String[12];
		populateMonths();

		days = new String[7];
		populateDays();

	}
}