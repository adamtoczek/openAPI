# openAPI
Tests are written as unit tests in jUnit 5. They are independent from each other. They test:
- CRUD operations and logic
- API validation
## How to run the tests
Run 
```bash
mvn clean test
```
## Test report
Using Allure, after test execution run
```bash
allure serve .\target\surefire-reports
```
