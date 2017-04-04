# Bellman–Ford Algorithm

This project is for implementing Bellman–Ford Algorithm to find the single source shortest paths in asynchronous distributed systems

Execution:

JAVAC AsynchBellmanFord.java

JAVA AsynchBellmanFord <path_to_file>

Sample Input file contents:(connectivity.txt)

9	1
-1	4	8	-1	-1	-1	-1	-1	-1
4	-1	11	-1	8	-1	-1	-1	-1
8	11	-1	7	-1	1	-1	-1	-1
-1	-1	7	-1	2	6	-1	-1	-1
-1	8	-1	2	-1	-1	7	4	-1
-1	-1	1	6	-1	-1	-1	2	-1
-1	-1	-1	-1	7	-1	-1	14	9
-1	-1	-1	-1	4	2	14	-1	10
-1	-1	-1	-1	-1	-1	9	10	-1

Sample output:

Bellman Ford Algorithm Executed !!
Following is the resultant Shortest Path Tree as a Connectivity Matrix: 
0	4	8	-1	-1	-1	-1	-1	-1	
4	-1	-1	-1	8	-1	-1	-1	-1	
8	-1	-1	-1	-1	1	-1	-1	-1	
-1	-1	-1	-1	2	-1	-1	-1	-1	
-1	8	-1	2	-1	-1	7	-1	-1	
-1	-1	1	-1	-1	-1	-1	2	-1	
-1	-1	-1	-1	7	-1	-1	-1	-1	
-1	-1	-1	-1	-1	2	-1	-1	10	
-1	-1	-1	-1	-1	-1	-1	10	-1	
Process: 7; Parent: 5; Distance from Source: 19

Process: 5; Parent: 2; Distance from Source: 12

Process: 1; Parent: 1; Distance from Source: 0

Process: 8; Parent: 6; Distance from Source: 11

Process: 2; Parent: 1; Distance from Source: 4

Process: 4; Parent: 5; Distance from Source: 14

Process: 3; Parent: 1; Distance from Source: 8

Process: 9; Parent: 8; Distance from Source: 21

Process: 6; Parent: 3; Distance from Source: 9

