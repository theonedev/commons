package io.onedev.commons.loader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public abstract class AppLoaderMocker {

	private MockedStatic<AppLoader> mockedStatic;
	
    @BeforeEach
    public void before() {
        MockitoAnnotations.openMocks(this);
        mockedStatic = Mockito.mockStatic(AppLoader.class);
        
        setup();
    }
    
    @AfterEach
    public void after() {
    	mockedStatic.close();
        teardown();
    }
    
    protected abstract void setup();
    
    protected abstract void teardown();

}
