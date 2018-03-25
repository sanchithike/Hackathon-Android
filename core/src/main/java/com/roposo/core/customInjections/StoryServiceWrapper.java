package com.roposo.core.customInjections;

/**
 * Created by bajaj on 16/05/17.
 */

public class StoryServiceWrapper {
    public static void setStoryServiceImpl(StoryServiceWrapper.StoryServiceImp storyServiceImpl) {
        StoryServiceWrapper.storyServiceImpl= storyServiceImpl;
    }

    public static void run() {
        storyServiceImpl.run(null);
    }

    public static void run(String storyId) {
        storyServiceImpl.run(storyId);
    }

    public static StoryServiceWrapper.StoryServiceImp storyServiceImpl = new StoryServiceImp() {
        public void run(String storyId) {
        }
    };

    public interface StoryServiceImp {
        void run(String storyid);
    }
}
