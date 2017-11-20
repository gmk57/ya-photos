package gmk57.yaphotos.data.source;

import android.support.annotation.NonNull;

import org.greenrobot.eventbus.EventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;

import java.util.concurrent.CountDownLatch;

import gmk57.yaphotos.AlbumLoadedEvent;
import gmk57.yaphotos.data.Album;
import gmk57.yaphotos.data.Photo;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class AlbumRepositoryTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Mock
    private LocalSource mLocalSource;
    @Mock
    private NetworkSource mNetworkSource;
    @Mock
    private EventBus mEventBus;
    @InjectMocks
    private AlbumRepository mAlbumRepository;
    private Album mAlbum;
    private CountDownLatch mDoneLatch;
    private Answer mDone = new DoneAnswer();

    @Before
    public void setUp() throws Exception {
        mAlbum = new Album();
        mAlbum.addPhoto(new Photo());
        mDoneLatch = new CountDownLatch(1);
    }

    @Test
    public void getAlbum_FirstTime_ReturnsEmptyAlbum() throws Exception {
        Album album = mAlbumRepository.getAlbum(2);
        assertThat(album, is(notNullValue()));
        assertThat(album.getSize(), is(0));
    }

    @Test
    public void getAlbum_FromDb() throws Exception {
        Album album = getAlbumTwice(mLocalSource.fetchAlbum(2), false);

        assertThat(album.getSize(), is(1));
        verifyZeroInteractions(mNetworkSource);
    }

    @Test
    public void getAlbum_FetchesDbOnlyOnce() throws Exception {
        getAlbumTwice(mLocalSource.fetchAlbum(2), false);
        Thread.sleep(100);  // We can't use CountDownLatch here

        verify(mLocalSource).fetchAlbum(2);
        verifyNoMoreInteractions(mLocalSource);  // No writes to DB
    }

    @Test
    public void getAlbum_DbEmpty_FetchesNetwork() throws Exception {
        Album album = getAlbumTwice(mNetworkSource.fetchAlbum(2, ""), false);

        assertThat(album.getSize(), is(1));
    }

    @Test
    public void getAlbum_PostsProperEvent() throws Exception {
        getAlbumTwice(mLocalSource.fetchAlbum(2), false);

        verify(mEventBus).post(argThat(event -> ((AlbumLoadedEvent) event).getAlbumType() == 2));
    }

    @Test
    public void getAlbum_FromNetwork_SavesToDb() throws Exception {
        getAlbumTwice(mNetworkSource.fetchAlbum(2, ""), true);

        verify(mLocalSource).saveAlbum(mAlbum, 2);
        verify(mLocalSource).savePhotos(argThat(list -> list.size() == 1), eq(2L));
        verify(mLocalSource, never()).clearPhotos(anyLong());
    }

    @Test
    public void getAlbum_AllFailed_TriesAgain() throws Exception {
        doAnswer(mDone).when(mEventBus).post(any());
        mAlbumRepository.getAlbum(2);  // First call fires background thread
        assertTrue("Done signal did not arrive", mDoneLatch.await(5, SECONDS));

        mDoneLatch = new CountDownLatch(1);
        mAlbumRepository.getAlbum(2); // Second call should fire thread again
        assertTrue("Done signal did not arrive", mDoneLatch.await(5, SECONDS));

        verify(mLocalSource, times(2)).fetchAlbum(anyLong());
        verify(mNetworkSource, times(2)).fetchAlbum(2, "");
        verifyNoMoreInteractions(mLocalSource);  // No writes to DB
    }

    @Test
    public void reloadAlbum_FetchesFromNetwork() throws Exception {
        doAnswer(mDone).when(mEventBus).post(any());

        mAlbumRepository.reloadAlbum(2);
        assertTrue("Done signal did not arrive", mDoneLatch.await(5, SECONDS));

        verify(mNetworkSource).fetchAlbum(2, "");
        verifyZeroInteractions(mLocalSource);
    }

    @Test
    public void reloadAlbum_SavesToMemoryAndDb() throws Exception {
        when(mNetworkSource.fetchAlbum(2, "")).thenReturn(mAlbum);
        doAnswer(mDone).when(mEventBus).post(any());

        mAlbumRepository.reloadAlbum(2);
        assertTrue("Done signal did not arrive", mDoneLatch.await(5, SECONDS));
        Album album = mAlbumRepository.getAlbum(2);

        assertThat(album.getSize(), is(1));
        InOrder inOrder = inOrder(mLocalSource);
        inOrder.verify(mLocalSource).clearPhotos(2);
        inOrder.verify(mLocalSource).saveAlbum(mAlbum, 2);
        inOrder.verify(mLocalSource).savePhotos(argThat(list -> list.size() == 1), eq(2L));
    }

    @Test
    public void reloadAlbum_Failed_KeepsOldAlbum() throws Exception {
        getAlbumTwice(mLocalSource.fetchAlbum(2), false);

        mAlbumRepository.reloadAlbum(2);
        Album album = mAlbumRepository.getAlbum(2);
        Thread.sleep(100);  // We can't use CountDownLatch here

        assertThat(album.getSize(), is(1));
        verify(mLocalSource).fetchAlbum(2);
        verifyNoMoreInteractions(mLocalSource);  // No writes to DB
    }

    @Test
    public void fetchNextPage_OffsetNotNull_FetchesFromNetwork() throws Exception {
        mAlbum.setNextOffset("fake_offset");
        getAlbumTwice(mLocalSource.fetchAlbum(2), false);

        mDoneLatch = new CountDownLatch(1);
        mAlbumRepository.fetchNextPage(2);
        assertTrue("Done signal did not arrive", mDoneLatch.await(5, SECONDS));

        verify(mNetworkSource).fetchAlbum(2, "fake_offset");
    }

    @Test
    public void fetchNextPage_OffsetNull_DoesNothing() throws Exception {
        getAlbumTwice(mLocalSource.fetchAlbum(2), false);

        mAlbumRepository.fetchNextPage(2);
        Thread.sleep(100);  // We can't use CountDownLatch here

        verifyZeroInteractions(mNetworkSource);
    }

    @Test
    public void fetchNextPage_AppendsToOldAlbum() throws Exception {
        mAlbum.setNextOffset("fake_offset");
        Album nextPage = new Album();
        nextPage.addPhoto(new Photo());
        nextPage.addPhoto(new Photo());
        when(mNetworkSource.fetchAlbum(2, "fake_offset")).thenReturn(nextPage);
        getAlbumTwice(mLocalSource.fetchAlbum(2), false);

        mDoneLatch = new CountDownLatch(1);
        mAlbumRepository.fetchNextPage(2);
        assertTrue("Done signal did not arrive", mDoneLatch.await(5, SECONDS));
        Album album = mAlbumRepository.getAlbum(2); // Check results

        assertThat(album.getSize(), is(3));
        assertThat(album.getNextOffset(), is(nullValue()));
        verify(mLocalSource).saveAlbum(nextPage, 2);
        verify(mLocalSource).savePhotos(argThat(list -> list.size() == 2), eq(2L));
        verify(mLocalSource, never()).clearPhotos(anyLong());
    }

    @Test
    public void fetchNextPage_Failed_KeepsOldAlbum() throws Exception {
        mAlbum.setNextOffset("fake_offset");
        getAlbumTwice(mLocalSource.fetchAlbum(2), false);

        mAlbumRepository.fetchNextPage(2);
        Album album = mAlbumRepository.getAlbum(2);
        Thread.sleep(100);  // We can't use CountDownLatch here

        assertThat(album.getSize(), is(1));
        verify(mLocalSource).fetchAlbum(2);
        verifyNoMoreInteractions(mLocalSource);  // No writes to DB
    }

    @Test
    public void fetchNextPage_AlreadyRunning_DoesNothing() throws Exception {
        mAlbum.setNextOffset("fake_offset");
        getAlbumTwice(mLocalSource.fetchAlbum(2), false);

        mAlbumRepository.fetchNextPage(2);
        mAlbumRepository.fetchNextPage(2);
        mAlbumRepository.fetchNextPage(2);
        Thread.sleep(100);

        verify(mNetworkSource).fetchAlbum(2, "fake_offset");
    }

    /**
     * Convenience method to reduce boilerplate. Fires {@code getAlbum} twice: first time to start
     * background fetch, waits for its completion, then second time to check results.
     *
     * @param methodCallToReturnAlbum Mock method call that should return {@code mAlbum}
     * @param shouldWaitForSaving     True if call to {@link LocalSource#savePhotos} is expected
     *                                and {@code mDoneLatch} should wait for it to complete
     * @return Result of second {@code getAlbum} invocation
     */
    @NonNull
    private Album getAlbumTwice(Album methodCallToReturnAlbum, boolean shouldWaitForSaving)
            throws InterruptedException {
        when(methodCallToReturnAlbum).thenReturn(mAlbum);

        if (shouldWaitForSaving) {  // Last action should be saving photos
            doAnswer(mDone).when(mLocalSource).savePhotos(any(), anyLong());
        } else {                    // Last action should be posting to EventBus
            doAnswer(mDone).when(mEventBus).post(any());
        }

        mAlbumRepository.getAlbum(2);  // First call fires background thread
        assertTrue("Done signal did not arrive", mDoneLatch.await(5, SECONDS));
        return mAlbumRepository.getAlbum(2); // Second call checks result
    }

    /**
     * Custom Answer to release {@code mDoneLatch} from mock methods
     */
    private class DoneAnswer implements Answer {
        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
            mDoneLatch.countDown();
            return null;
        }
    }
}
