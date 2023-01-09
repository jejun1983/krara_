package com.idevel.krara.interfaces

/**
 * IDataSaverListener
 * @company : medialog
 * @author : jjbae
 * datasaver 상태변경 알림 인터페이스
 **/
interface SimStateChangeListener {
    fun onUsimMount(){}
    fun onUsimUnMount(){}
}