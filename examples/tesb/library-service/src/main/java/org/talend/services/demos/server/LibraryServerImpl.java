package org.talend.services.demos.server;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Resource;
import javax.xml.ws.WebServiceContext;

import org.springframework.beans.factory.InitializingBean;
import org.talend.esb.mep.requestcallback.feature.CallContext;
import org.talend.services.demos.common.Utils;
import org.talend.services.demos.library._1_0.Library;
import org.talend.services.demos.library._1_0.LibraryConsumer;
import org.talend.services.demos.library._1_0.SeekBookError;
import org.talend.types.demos.generalobjects.errorhandling._1.ExceptionFrame;
import org.talend.types.demos.generalobjects.errorhandling._1.ExceptionType;
import org.talend.types.demos.library.common._1.BookType;
import org.talend.types.demos.library.common._1.ListOfBooks;
import org.talend.types.demos.library.common._1.PersonType;
import org.talend.types.demos.library.common._1.SearchFor;
import org.talend.types.demos.library.common._1.SearchInBasementFor;

public class LibraryServerImpl implements Library, InitializingBean {
	
    @Resource
    private WebServiceContext wsContext;

	private LibraryConsumer callbackResponseClient;
	private LibraryPublisher libraryPublisher;
		
	public void setCallbackResponseClient(LibraryConsumer callbackResponseClient) {
		this.callbackResponseClient = callbackResponseClient;
	}

	public void setLibraryPublisher(LibraryPublisher libraryPublisher) {
		this.libraryPublisher = libraryPublisher;
	}

	@Override
	public void createLending(String isbnNumber, Date dateOfBirth, String zip,
			Date borrowed) {
		
    	System.out.println("***************************************************************");          
        System.out.println("*** createLending request (Oneway operation) is received ******");
        System.out.println("***************************************************************"); 
        
		System.out.println("Lending request:");
		
        Utils.showLendingRequest(isbnNumber, dateOfBirth, zip, borrowed);        
	}

	@Override
	public ListOfBooks seekBook(SearchFor body) throws SeekBookError {
		
    	System.out.println("***************************************************************");          
        System.out.println("*** seekBook request (Request-Response operation) is received *");
        System.out.println("***************************************************************");         
        
        showSeekBookRequest(body);
	
		List<String> authorsLastNames = body.getAuthorLastName();
		if (authorsLastNames != null && authorsLastNames.size() > 0) {
			String authorsLastName = authorsLastNames.get(0);
			if (authorsLastName != null && authorsLastName.length() > 0 &&
					!"Icebear".equalsIgnoreCase(authorsLastName)) {
				SeekBookError e = prepareException("No book available from author " + authorsLastName);
				
				System.out.println("No book available from author " +  authorsLastName);
				System.out.println("\nSending business fault (SeekBook error) with parameters:");
				
				Utils.showSeekBookError(e);
				
				throw e;
			}
		}
		ListOfBooks result = new ListOfBooks();
		BookType book = new BookType();
		result.getBook().add(book);
		PersonType author = new PersonType();
		book.getAuthor().add(author);
		author.setFirstName("Jack");
		author.setLastName("Icebear");
		Calendar dateOfBirth = new GregorianCalendar(101, Calendar.JANUARY, 2);
		author.setDateOfBirth(dateOfBirth.getTime());
		book.getTitle().add("Survival in the Arctic");
		book.getPublisher().add("Frosty Edition");
		book.setYearPublished("2010");
		
		System.out.println("Book(s) is found:");
		
		showSeekBookResponse(result);
		
		return result;
	}
	
	@Override
	public void seekBookInBasement(SearchInBasementFor body) {
        System.out.println("****************************************************************************");
        System.out.println("*** seekBookInBasement request (Request-Callback operation) was received ***");
        System.out.println("****************************************************************************");

        showSeekBookInBasementRequest(body);

        CallContext ctx = CallContext.getCallContext(wsContext.getMessageContext());
        
        System.out.println("Info from CallContext:");
        System.out.println("- Call ID is " + ctx.getCallId());
                
        List<String> authorsLastNames = body.getAuthorLastName();
        if (authorsLastNames != null && authorsLastNames.size() > 0) {
            String authorsLastName = authorsLastNames.get(0);
            if (authorsLastName != null && authorsLastName.length() > 0
                    && !"Stripycat".equalsIgnoreCase(authorsLastName)) {

                SeekBookError e = prepareException("No book available from author "
                        + authorsLastName);

                System.out.println("No book available from author "
                        + authorsLastName);
                System.out
                        .println("\nSending business fault (SeekBook error) with parameters:");

                Utils.showSeekBookError(e);

                LibraryConsumer libraryConsumer = ctx.createCallbackProxy(LibraryConsumer.class);
                libraryConsumer.seekBookInBasementFault(e.getFaultInfo());
            }
        }

        ListOfBooks result = new ListOfBooks();
        BookType book = new BookType();
        result.getBook().add(book);
        PersonType author = new PersonType();
        book.getAuthor().add(author);
        author.setFirstName("John");
        author.setLastName("Stripycat");
        Calendar dateOfBirth = new GregorianCalendar(202, Calendar.MAY, 17);
        author.setDateOfBirth(dateOfBirth.getTime());
        book.getTitle().add("Hunting basement inhabitants");
        book.getPublisher().add("Dusty Edition");
        book.setYearPublished("2013");

        System.out.println("Book(s) is found:");

        showSeekBookResponse(result);

		ctx.setupCallbackProxy(callbackResponseClient);
		callbackResponseClient.seekBookInBasementResponse(result);
	}

	@Override
	public void newBooks(Date listDate, List<BookType> book) {
		// Client, not service will receive notification
		throw new UnsupportedOperationException();
	}

	private void showSeekBookRequest(final SearchFor request){
    	if(request == null){
    		System.out.println("Request body is empty");
    		return;
    	}
    	System.out.println("Autors last name in request: " );
    	List<String> authorLastName = request.getAuthorLastName();
    	for (String name : authorLastName) {
			System.out.println(name);
		}
    }

	private void showSeekBookResponse(final ListOfBooks response ){
    	Utils.showBooks(response);
    }    
    
    private void showSeekBookInBasementRequest(final SearchInBasementFor request){
        if(request == null){
            System.out.println("Request body is empty");
            return;
        }
        System.out.println("Autors last name in request: " );
        List<String> authorLastName = request.getAuthorLastName();
        for (String name : authorLastName) {
            System.out.println(name);
        }
    }

    private SeekBookError prepareException(String message) {
		ExceptionType exception = new ExceptionType();
		exception.setOperation("seekBook");
		exception.setServiceName("LibraryService");				
		exception.setExceptionText(message);
		ExceptionFrame frame = new ExceptionFrame();
		frame.getException().add(exception);
		SeekBookError e = new SeekBookError("Book not found", frame);
		return e;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(1);
        Runnable publisher = new Runnable() {
			@Override
			public void run() {
		        try {
					libraryPublisher.publishNewBooksNotifications();
				} catch (InterruptedException e) {
					throw new RuntimeException("Notification is interrupted: " + e.getMessage(), e);
				}
			}
        };
	    executor.execute(publisher);
    }

}