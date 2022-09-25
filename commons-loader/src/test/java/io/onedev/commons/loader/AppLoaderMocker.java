package io.onedev.commons.loader;

import org.junit.After;
import org.junit.Before;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public abstract class AppLoaderMocker {

	private MockedStatic<AppLoader> mockedStatic;
	
    @Before
    public void before() {
        MockitoAnnotations.openMocks(this);
        mockedStatic = Mockito.mockStatic(AppLoader.class);
        
        setup();
    }
    
    @After
    public void after() {
    	mockedStatic.close();
        teardown();
    }
    
    protected abstract void setup();
    
    protected abstract void teardown();

}
