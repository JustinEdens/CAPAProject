package com.example.capaproject

import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import com.example.capaproject.SurveyReaderContract.SurveyEntry
import com.example.capaproject.WorkReaderContract.WorkEntry
import com.example.capaproject.UserPrefsContract.UserPrefsEntry

//import com.example.capaproject.StateReaderContract.StateEntry


const val WORK_TABLE_NAME = "atWork"

object SurveyReaderContract{
    object SurveyEntry : BaseColumns{
        const val TABLE_NAME = "Survey"
        const val COLUMN_QUESTION = "Question"
        const val COLUMN_ANSWER = "Answer"
    }
}

/*object StateReaderContract{
    object StateEntry : BaseColumns{
        //const val TABLE_NAME = ""
        const val COLUMN_PACKAGE = "Package"
        const val COLUMN_CLASS = "Class"
        const val COLUMN_WEIGHT = "Weight"
    }
}*/

object WorkReaderContract{
    object WorkEntry : BaseColumns{
        const val TABLE_NAME = "Work"
        const val COLUMN_PACKAGE = "Package"
        const val COLUMN_CLASS = "Class"
        const val COLUMN_WEIGHT = "Weight"
    }
}

object UserPrefsContract{
    object UserPrefsEntry : BaseColumns{
        const val TABLE_NAME = "UserPrefs"
        const val COLUMN_PACKAGE = "Package"
        const val COLUMN_CLASS = "Class"
    }
}

private const val SURVEY_CREATE_ENTRIES =
    "CREATE TABLE IF NOT EXISTS ${SurveyEntry.TABLE_NAME} (" +
            "${BaseColumns._ID} INTEGER PRIMARY KEY," +
            "${SurveyEntry.COLUMN_QUESTION} TEXT," +
            "${SurveyEntry.COLUMN_ANSWER} TEXT)"

private const val USER_PREFS_CREATE_ENTRIES =
    "CREATE TABLE IF NOT EXISTS ${UserPrefsEntry.TABLE_NAME} (" +
            "${BaseColumns._ID} INTEGER PRIMARY KEY," +
            "${UserPrefsEntry.COLUMN_PACKAGE} TEXT," +
            "${UserPrefsEntry.COLUMN_CLASS} TEXT"

private const val WORK_CREATE_ENTRIES =
    "CREATE TABLE IF NOT EXISTS ${WorkEntry.TABLE_NAME} (" +
            "${BaseColumns._ID} INTEGER PRIMARY KEY," +
            "${WorkEntry.COLUMN_PACKAGE} TEXT," +
            "${WorkEntry.COLUMN_CLASS} TEXT," +
            "${WorkEntry.COLUMN_WEIGHT} DOUBLE)"

private const val SURVEY_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${SurveyEntry.TABLE_NAME}"
private const val WORK_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${WorkEntry.TABLE_NAME}"
private const val USER_PREFS_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${UserPrefsEntry.TABLE_NAME}"

class DatabaseHandler(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SURVEY_CREATE_ENTRIES)
        db.execSQL(WORK_CREATE_ENTRIES)
        db.execSQL(USER_PREFS_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(SURVEY_DELETE_ENTRIES)
        db.execSQL(WORK_DELETE_ENTRIES)
        db.execSQL(USER_PREFS_DELETE_ENTRIES)
        onCreate(db)
    }
    companion object{
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "Database"
    }

    //Adds or updates user preferences in database
    fun updateUserPrefsInfo(prefs: UserPrefApps){
        val db = this.writableDatabase
        db.execSQL(USER_PREFS_CREATE_ENTRIES)

        val clock = prefs.getAttr("Clock")
        val music = prefs.getAttr("Music")

        val clockPKG = clock.packageName
        val clockCLS = clock.className

        val musicPKG = music.packageName
        val musicCLS = music.className

        val values = ContentValues().apply {
            put(UserPrefsEntry.COLUMN_PACKAGE, clockPKG)
            put(UserPrefsEntry.COLUMN_CLASS, clockCLS)
            put(UserPrefsEntry.COLUMN_PACKAGE, musicPKG)
            put(UserPrefsEntry.COLUMN_CLASS, musicCLS)
        }
        db.replace(SurveyEntry.TABLE_NAME, null, values)

        db.close()
    }

    //Adds or updates work state info in database
    private fun updateWorkInfo(map: HashMap<ComponentName, Double>){
        val db = this.writableDatabase
        db.execSQL(WORK_DELETE_ENTRIES)
        db.execSQL(WORK_CREATE_ENTRIES)

        for(entry in map){
            val pkg = entry.key.packageName
            val cls = entry.key.className
            val weight = entry.value
            //val selectQuery = "SELECT * FROM ${WorkEntry.TABLE_NAME}"

            val values = ContentValues().apply{
                put(WorkEntry.COLUMN_PACKAGE, pkg)
                put(WorkEntry.COLUMN_CLASS, cls)
                put(WorkEntry.COLUMN_WEIGHT, weight)
            }
            db.insert(WorkEntry.TABLE_NAME, null, values)

            /*val cursor = db.rawQuery(selectQuery, null)
            var exists = false
            var needsDeletion = false
            cursor.moveToFirst()
            while(!cursor.isAfterLast) {
                //db.replace(WorkEntry.TABLE_NAME, null, values)
                if(cursor.getColumnName(cursor.getColumnIndex(WorkEntry.COLUMN_CLASS)) == cls){
                    //db.update(WorkEntry.TABLE_NAME, values, null, null)
                    //cursor.moveToNext()
                    exists = true
                    needsDeletion = false
                    break
                }else if(cursor.getColumnName(cursor.getColumnIndex(WorkEntry.COLUMN_CLASS)) != cls){
                    needsDeletion = true

                }
                cursor.moveToNext()
            }

            if(exists){
                db.update(WorkEntry.TABLE_NAME, values, "${WorkEntry.COLUMN_CLASS}=$cls", null)
            }else if(needsDeletion){
                db.delete(WorkEntry.TABLE_NAME, null, null)
            }*/
        }
        db.close()
    }

    //Updates state info in corresponding table using passed string to check which state
    fun updateState(stateName: String, map: HashMap<ComponentName, Double>){
        when(stateName){
            "atWork" -> updateWorkInfo(map)
        }
    }

    /*fun addState(stateName: String, map: HashMap<ComponentName, Double>){
        if(stateName == "Work"){
            addWorkState(map)
        }
    }*/

    //Deletes all tables in database
    fun deleteInfo(){
        val db = this.writableDatabase
        onUpgrade(db, 1, 1)
    }

    //Adds or updates survey info in database
    fun updateSurveyInfo(profile: UserProfile){
        val db = this.writableDatabase
        db.execSQL(SURVEY_CREATE_ENTRIES)

        for(entry in profile.getFieldNames()){
            val question = entry
            val answer = profile.getField(entry)

            val values = ContentValues().apply{
                put(SurveyEntry.COLUMN_QUESTION, question)
                put(SurveyEntry.COLUMN_ANSWER, answer)
            }
            db.replace(SurveyEntry.TABLE_NAME, null, values)
        }
        db.close()
    }

    //Uses passed string to get info from corresponding table
    fun getStateInfo(stateName: String): HashMap<ComponentName, Double>? {
        return when (stateName) {
            "atWork" -> getWorkInfo()
            else -> return null
        }
    }

    //Gets work state info from database and returns as HashMap
    private fun getWorkInfo(): HashMap<ComponentName, Double> {
        val db = this.writableDatabase
        db.execSQL(WORK_CREATE_ENTRIES)

        val map: HashMap<ComponentName, Double> = HashMap()
        val selectQuery = "SELECT * FROM ${WorkEntry.TABLE_NAME}"
        val cursor = db.rawQuery(selectQuery, null)
        cursor!!.moveToFirst()
        while(!cursor.isAfterLast){
            val pkg = cursor.getString(cursor.getColumnIndex("Package"))
            val cls = cursor.getString(cursor.getColumnIndex("Class"))
            val weight = cursor.getDouble(cursor.getColumnIndex("Weight"))
            val compName = ComponentName(
                pkg,
                cls
            )
            map[compName] = weight
            cursor.moveToNext()
        }
        cursor.close()
        return map
    }

    //Gets survey info from database and returns as HashMap
    fun getSurveyInfo(): HashMap<String, String>{
        val db = this.readableDatabase
        db.execSQL(SURVEY_CREATE_ENTRIES)
        val map: HashMap<String, String> = HashMap()
        val selectQuery = "SELECT * FROM ${SurveyEntry.TABLE_NAME}"
        val cursor = db.rawQuery(selectQuery, null)
        cursor!!.moveToFirst()
        while(!cursor.isAfterLast){
            val question = cursor.getString(cursor.getColumnIndex(SurveyEntry.COLUMN_QUESTION))
            val answer = cursor.getString(cursor.getColumnIndex(SurveyEntry.COLUMN_ANSWER))
            map[question] = answer
            cursor.moveToNext()
        }
        cursor.close()
        return map
    }
}
