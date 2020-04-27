package com.waveformhealth.di

import com.waveformhealth.MainActivity
import com.waveformhealth.room.RoomFragment
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class
    ]
)
interface AppComponent {
    fun inject(activity: MainActivity)
    fun inject(roomFragment: RoomFragment)
}