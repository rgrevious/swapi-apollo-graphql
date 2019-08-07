# The Federation Records
An Android graphql client for the swapi endpoint https://swapi.co  

### App Features: 

-List and cache Star Wars characters  
-Show some details about each character offline  
-Filter Star Wars characters by first or last name  
-List and cache Star Wars films by a featured character  
-Filter Star Wars films by title  
-Show some details about each film offline  


### Internal Features:  

-Creates a single ApolloClient instance that can cache query results  
-A swapi graphql end point was deployed for this assignment at https://swapi-graphql-demo.herokuapp.com/  
-Static queries in .graphql format that can be created and used by other software platforms 

Internal Notes:  
Master/detail fragments can also by children depending on the navigation tree   

### Runtime Requirements:  
An initial connection is required, when the app is installed for the first time, to cache the results.
Once the results are shown, the phone can go offline and continue browsing the results. The connection 
timeout is set to 8 seconds. 

###  Test Case 1:  
The mobile phone is connected to the internet.  
Install and run the app.  

Expected Result:  
An app called SWAPI Graphql Demo appears in the app list  
A list of Star Wars characters appears  with the title "The Federation Records". 
Since herokuapp is a free service, response times may vary. 


###  Test Case 2:  
The mobile phone is offline but has already run the app at least once while online. 

Expected Result:  
A list of Star Wars characters appears.  Each character can show details and films they are featured in.    


###  Test Case 3:  
The mobile phone is not connected to the internet and never ran the SWAPI Graphql Demo app.  
Install and run the app.  

Expected Result:  
An app called SWAPI Graphql Demo appears in the app list.    
An empty list appears on the home screen with the title "The Federation Records"  

Recovery step:  
Please close the  SWAPI Graphql Demo app and connect the mobile phone to the internet.  
Run the SWAPI Graphql Demo app again.  


### Build Instructions:
Load the Android Studio project from the existing project directory in 
swapi-apollo-graphql/src/apps/android/starwars.    
Android Studio should start syncing and downloading the apollo graphql dependencies.  
apollo-gradle-plugin  
apollo-runtime  
apollo-android-support  


### Android Studio Project directory:  
/swapi-apollo-graphql/src/apps/android/starwars 

.graphql files:  
/swapi-apollo-graphql/src/apps/android/starwars/app/src/main/graphql/com/apollographql/apollo/swapi

Output:  
/swapi-apollo-graphql/src/apps/android/starwars/app/build/outputs/apk/debug/app-debug.apk
