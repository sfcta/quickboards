@echo off
java -Xms64m -Xmx512m -cp y:\champ\util\bin\sfcta.jar;y:\champ\util\bin\quickboards.jar org.sfcta.quickboards.QuickBoards %1 %2
