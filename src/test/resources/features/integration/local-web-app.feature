@regression
Feature: Local web app feature 1

  Scenario: Ensure that local web app page 1 can be opened
    When I open local web app page 1
    Then local web app page 1 is displayed

  Scenario: Ensure that local web app page 2 can be opened
    When I open local web app page 2
    Then local web app page 2 is displayed

  @excluded
  Scenario: This scenario will be excluded
    When I open local web app page 1
    Then local web app page 1 is displayed
