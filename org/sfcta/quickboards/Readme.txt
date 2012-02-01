from above "org" dir:

To compile:
set JARNAME=qbtemp
set JARDIR=[directory where you're running these commands]

javac -cp Y:\champ\util\bin\sfcta.jar org\sfcta\quickboards\*.java
jar -cvfm %JARNAME%.jar org\sfcta\quickboards\Manifest.txt org\sfcta\quickboards\*.class
jar -tvf %JARNAME%.jar

To run:
java -Xms64m -Xmx512m -cp "%JARDIR%\%JARNAME%.jar;Y:\champ\util\bin\sfcta.jar" org.sfcta.quickboards.QuickBoards quickboards.ctl quickboards_out.xls

To deploy:
Compile with JARNAME=quickboards
Copy the resulting quickboards.jar to Y:\champ\util\bin