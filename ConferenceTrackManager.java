package com.my.conference.model;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ConferenceTrackManager {

	/**
     * Constructor for ConferenceTrackManager.
     */
    public ConferenceTrackManager() {
		// TODO Auto-generated constructor stub
	}

    public List<List<Talk>> scheduleConference(String fileName) throws Exception
    {
        List<String> talkList = getTalkListFromFile(fileName);
        return scheduleConference(talkList);
    }


    /**
     * public method to create and schedule conference.
     * @param talkList
     * @throws InvalidTalkException
     */
    public List<List<Talk>> scheduleConference(List<String> talkList) throws Exception
    {
        List<Talk> talksList = validateAndCreateTalks(talkList);
        return getScheduleConferenceTrack(talksList);
    }

    /**
     * Load talk list from input file.
     * @param fileName
     * @return
     * @throws InvalidTalkException
     */
    public List<String> getTalkListFromFile(String fileName)
    {
        List<String> talkList = new ArrayList<String>();
        try{
          InputStream inputStream = new FileInputStream(fileName);
    	  InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
    	  BufferedReader br = new BufferedReader(inputStreamReader);
          String strLine = br.readLine();
          //Read File Line By Line
          while (strLine != null){
            talkList.add(strLine);
            strLine = br.readLine();
          }
          //Close the input stream
          br.close();
  		  inputStreamReader.close();
  		  inputStream.close();
        }catch (Exception e){//Catch exception if any
          System.err.println("Error: " + e.getMessage());
        }

        return talkList;

    }


    /**
     * Validate talk list, check the time for talk and initialize Talk Object accordingly.
     * @param talkList
     * @throws Exception
     */
    private List<Talk> validateAndCreateTalks(List<String> talkList) throws Exception
    {
        // If talksList is null throw exception invalid list to schedule.
        if(talkList == null)
            throw new InvalidTalkException("Empty Talk List");

        List<Talk> validTalksList = new ArrayList<Talk>();
        //int talkCount = -1;
        String minSuffix = "min";
        String lightningSuffix = "lightning";

        // Iterate list and validate time.
        for(String talk : talkList)
        {
            int lastSpaceIndex = talk.lastIndexOf(" ");
            // if talk does not have any space, means either title or time is missing.
            if(lastSpaceIndex == -1)
                throw new InvalidTalkException("Invalid talk, " + talk + ". Talk time must be specify.");

            String name = talk.substring(0, lastSpaceIndex);
            String timeStr = talk.substring(lastSpaceIndex + 1);
            // If title is missing or blank.
            if(name == null || "".equals(name.trim()))
                throw new InvalidTalkException("Invalid talk name, " + talk);
            // If time is not ended with min or lightning.
            else if(!timeStr.endsWith(minSuffix) && !timeStr.endsWith(lightningSuffix))
                throw new InvalidTalkException("Invalid talk time, " + talk + ". Time must be in min or in lightning");

            //talkCount++;
            int time = 0;
            // Parse time from the time string .
            try{
                if(timeStr.endsWith(minSuffix)) {
                    time = Integer.parseInt(timeStr.substring(0, timeStr.indexOf(minSuffix)));
                }
                else if(timeStr.endsWith(lightningSuffix)) {
                    String lightningTime = timeStr.substring(0, timeStr.indexOf(lightningSuffix));
                    if("".equals(lightningTime))
                        time = 5;
                    else
                        time = Integer.parseInt(lightningTime) * 5;
                }
            }catch(NumberFormatException nfe) {
                throw new InvalidTalkException("Unbale to parse time " + timeStr + " for talk " + talk);
            }

            // Add talk to the valid talk List.
            validTalksList.add(new Talk(talk, name, time));
        }

        return validTalksList;
    }


    /**
     * Schedule Conference tracks for morning and evening session.
     * @param talksList
     * @throws Exception
     */
    private List<List<Talk>> getScheduleConferenceTrack(List<Talk> talksList) throws Exception
    {
        // Find the total possible days.
    	int perDayMinTime = 7 * 60;
        int totalTalksTime = getTotalTalksTime(talksList);
        int totalPossibleDays = totalTalksTime/perDayMinTime;

        if( totalTalksTime % perDayMinTime != 0)
        	totalPossibleDays++;

        //System.out.println("totalTalksTime = " + totalTalksTime + " totalPossibleDays =" + totalPossibleDays);

        // Sort the talkList.
        List<Talk> talksListForOperation = new ArrayList<Talk>();
        talksListForOperation.addAll(talksList);
        Collections.sort(talksListForOperation);


        // Find possible combinations for the morning session.
        List<List<Talk>> combForMornSessions = findPossibleCombSession(talksListForOperation, totalPossibleDays, true, false);


        // Remove all the scheduled talks for morning session, from the operationList.
        for(List<Talk> talkList : combForMornSessions) {
            talksListForOperation.removeAll(talkList);
        }


        // Find possible combinations for the evening session.
        List<List<Talk>> combForEveSessions = findPossibleCombSession(talksListForOperation, totalPossibleDays, false, false);


        // Remove all the scheduled talks for evening session, from the operationList.
        for(List<Talk> talkList : combForEveSessions) {
            talksListForOperation.removeAll(talkList);
        }


        // check if the operation list is not empty, then try to fill all the remaining talks in evening session.
        int maxSessionTimeLimit = 240;
        if(!talksListForOperation.isEmpty()) {

            for(List<Talk> talkList : combForEveSessions) {
                int totalTime = getTotalTalksTime(talkList);

                List<Talk> scheduledTalkList = new ArrayList<Talk>();

                for(Talk talk : talksListForOperation) {
                    int talkTime = talk.getTimeDuration();

                    if(talkTime + totalTime <= maxSessionTimeLimit) {

                    	talkList.add(talk);

                    	talk.setScheduled(true);
                        scheduledTalkList.add(talk);

                        totalTime= talkTime + totalTime;
                       }
                }

                talksListForOperation.removeAll(scheduledTalkList);
                if(talksListForOperation.isEmpty())
                    break;
            }
        }


        if(!talksListForOperation.isEmpty() && totalPossibleDays > 1 && combForEveSessions.size() < totalPossibleDays ){
        	List<List<Talk>> combForEveSessionsExtra = findPossibleCombSession(talksListForOperation, 2, false, true);
        	combForEveSessions.addAll(combForEveSessionsExtra);

        	for(List<Talk> talkList : combForEveSessionsExtra) {
                talksListForOperation.removeAll(talkList);
            }

        }


        // If operation list is still not empty, add morning and afternoon sessions
        if(!talksListForOperation.isEmpty())
        {
        	while(!talksListForOperation.isEmpty()){

        		List<List<Talk>> combForMornSessionsExtra = findPossibleCombSession(talksListForOperation, 1, true, false);
        		if(combForMornSessionsExtra.size() != 0){
        			combForMornSessions.addAll(combForMornSessionsExtra);

        			for(List<Talk> talkList : combForMornSessionsExtra) {
                        talksListForOperation.removeAll(talkList);
                    }

        		}

        		if(!talksListForOperation.isEmpty()){
        		    List<List<Talk>> combForEveSessionsExtra = findPossibleCombSession(talksListForOperation, 1, false, false);
            		    if(combForMornSessionsExtra.size() != 0){
            		        combForEveSessions.addAll(combForEveSessionsExtra);

            			for(List<Talk> talkList : combForEveSessionsExtra) {
                                    talksListForOperation.removeAll(talkList);
                                }
            		    }
        		}


        	}

        	//throw new Exception("Unable to schedule all task for conferencing.");
        }

        // Schedule the day event from morning session and evening session.
        return getScheduledTalksList(combForMornSessions, combForEveSessions);
    }

    /**
     * Find possible combination for the session.
     * If morning session then each session must have total time 3 hr.
     * if evening session then each session must have total time greater then 3 hr.
     * @param talksListForOperation
     * @param totalPossibleDays
     * @param morningSession
     * @return
     */
    private List<List<Talk>> findPossibleCombSession(List<Talk> talksListForOperation, int totalPossibleDays, boolean morningSession, boolean optimize)
    {
        int minSessionTimeLimit = 180;
        int maxSessionTimeLimit = 240;

        if(morningSession)
            maxSessionTimeLimit = minSessionTimeLimit;

        int talkListSize = talksListForOperation.size();

        List<List<Talk>> possibleCombinationsOfTalks = new ArrayList<List<Talk>>();
        int possibleCombinationCount = 0;

        // Loop to get combination for total possible days.
        // Check one by one from each talk to get possible combination.
        for(int count = 0; count < talkListSize; count++) {
            int startPoint = count;
            int totalTime = 0;
            List<Talk> possibleCombinationList = new ArrayList<Talk>();

            // Loop to get possible combination.
            while(startPoint != talkListSize) {
                int currentCount = startPoint;
                startPoint++;
                Talk currentTalk = talksListForOperation.get(currentCount);
                if(currentTalk.isScheduled())
                    continue;
                int talkTime = currentTalk.getTimeDuration();
                // If the current talk time is greater than maxSessionTimeLimit or
                // sum of the current time and total of talk time added in list  is greater than maxSessionTimeLimit.
                // then continue.
                if(talkTime > maxSessionTimeLimit || talkTime + totalTime > maxSessionTimeLimit) {
                    continue;
                }

                possibleCombinationList.add(currentTalk);
                totalTime += talkTime;

                // If total time is completed for this session than break this loop.
                if(morningSession) {
                    if(totalTime == maxSessionTimeLimit)
                        break;
                }else if(optimize && totalTime < maxSessionTimeLimit )
                    continue;
                else if(totalTime >= minSessionTimeLimit )
                	break;
            }

            // Valid session time for morning session is equal to maxSessionTimeLimit.
            // Valid session time for evening session is less than or equal to maxSessionTimeLimit and greater than or equal to minSessionTimeLimit.
            boolean validSession = false;
            validSession = (totalTime <= maxSessionTimeLimit);

            // If session is valid than add this session in the possible combination list and set all added talk as scheduled.
            if(validSession) {
                possibleCombinationsOfTalks.add(possibleCombinationList);
                for(Talk talk : possibleCombinationList){
                    talk.setScheduled(true);
                }
                possibleCombinationCount++;

                if(totalPossibleDays > 1){
                	if(morningSession && possibleCombinationCount == totalPossibleDays)
                		break;

                	//For Best fit
                    if(!morningSession && possibleCombinationCount == (totalPossibleDays - 1) )
                    	break;
                }

                else{
                	if(possibleCombinationCount == totalPossibleDays)
                		break;
                }

            }
        }

        return possibleCombinationsOfTalks;
    }

    /**
     * Print the scheduled talks with the expected text msg.
     * @param combForMornSessions
     * @param combForEveSessions
     */
	private List<List<Talk>> getScheduledTalksList(List<List<Talk>> combForMornSessions, List<List<Talk>> combForEveSessions)
    {
        List<List<Talk>> scheduledTalksList = new ArrayList<List<Talk>>();
        int totalPossibleDays = Math.max(combForEveSessions.size(), combForMornSessions.size()) ;

        // for loop to schedule event for all days.
        for(int dayCount = 0; dayCount < totalPossibleDays ; dayCount++) {
            List<Talk> talkList = new ArrayList<Talk>();

            // Create a date and initialize start time 09:00 AM.
            Date date = new Date( );
            SimpleDateFormat dateFormat = new SimpleDateFormat ("hh:mma ");

            //Using Calendar
            Calendar calendar = Calendar.getInstance();
    	    calendar.set(Calendar.HOUR_OF_DAY, 9);
    	    calendar.set(Calendar.MINUTE, 0);
    	    calendar.set(Calendar.SECOND, 0);
    	    date = calendar.getTime();


            int trackCount = dayCount + 1;
            String scheduledTime = dateFormat.format(date);

            System.out.println("Track " + trackCount + ":");

            if(trackCount <= combForMornSessions.size()  ){
            	// Morning Session - set the scheduled time in the talk and get the next time using time duration of current talk.
                List<Talk> mornSessionTalkList = combForMornSessions.get(dayCount);
                for(Talk talk : mornSessionTalkList) {
                    talk.setScheduledTime(scheduledTime);
                    System.out.println(scheduledTime + talk.getTitle());
                    scheduledTime = getNextScheduledTime(date, talk.getTimeDuration());
                    talkList.add(talk);
                }
            }

            if(trackCount == Math.max(combForEveSessions.size(), combForMornSessions.size()) && combForEveSessions.size() < combForMornSessions.size() )
            	return scheduledTalksList;
            else{
            	if( (scheduledTime.compareTo("12:00PM") < 0 ) || (scheduledTime.compareTo("12:00AM") < 0 ) ){
                    scheduledTime = "12:00PM ";

                    calendar.set(Calendar.HOUR_OF_DAY, 12);
            	    calendar.set(Calendar.MINUTE, 0);
            	    calendar.set(Calendar.SECOND, 0);
            	    date = calendar.getTime();

            	}
            }

            // Scheduled Lunch Time for 60 mins.
            int lunchTimeDuration = 60;
            Talk lunchTalk = new Talk("Lunch", "Lunch", 60);
            lunchTalk.setScheduledTime(scheduledTime);
            talkList.add(lunchTalk);
            System.out.println(scheduledTime + "Lunch");

            // Evening Session - set the scheduled time in the talk and get the next time using time duration of current talk.
            scheduledTime = getNextScheduledTime(date, lunchTimeDuration);
            List<Talk> eveSessionTalkList = combForEveSessions.get(dayCount);
            for(Talk talk : eveSessionTalkList) {
                talk.setScheduledTime(scheduledTime);
                talkList.add(talk);
                System.out.println(scheduledTime + talk.getTitle());
                scheduledTime = getNextScheduledTime(date, talk.getTimeDuration());
            }

            // Scheduled Networking Event at the end of session, Time duration is just to initialize the Talk object.
            Talk networkingTalk = new Talk("Networking Event", "Networking Event", 60);
            if(scheduledTime.compareTo("04:00PM") < 0){
            	if(combForMornSessions.size() == trackCount){
            		return scheduledTalksList;
            	}
            	else
            		scheduledTime = "04:00PM ";
            }

            networkingTalk.setScheduledTime(scheduledTime);
            talkList.add(networkingTalk);
            System.out.println(scheduledTime + "Networking Event\n");
            scheduledTalksList.add(talkList);

        }

        return scheduledTalksList;
    }

    /**
     * To get total time of talks of the given list.
     * @param talksList
     * @return
     */
    public static int getTotalTalksTime(List<Talk> talksList)
    {
        if(talksList == null || talksList.isEmpty())
            return 0;

        int totalTime = 0;
        for(Talk talk : talksList) {
            totalTime += talk.timeDuration;
        }
        return totalTime;
    }

    /**
     * To get next scheduled time in form of String.
     * @param date
     * @param timeDuration
     * @return
     */
    private String getNextScheduledTime(Date date, int timeDuration)
    {
        long timeInLong  = date.getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat ("hh:mma ");

        long timeDurationInLong = timeDuration * 60 * 1000;
        long newTimeInLong = timeInLong + timeDurationInLong;

        date.setTime(newTimeInLong);
        String str = dateFormat.format(date);
        return str;
    }

    /**
     * Main method to execute program.
     * @param args
     */
    public static void main(String[] args) {
    	String fileName = "C:\\Users\\abhi\\workspace\\Eclipse\\ConferenceTrackManager\\src\\com\\my\\conference\\data\\ConferenceData.txt";
        ConferenceTrackManager conferenceManager = new ConferenceTrackManager();
        try{
            conferenceManager.scheduleConference(fileName);
        }catch(InvalidTalkException ite) {
            ite.printStackTrace();
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }

}
