package es.smartaccess.SealSignSQSService.wcf;

//----------------------------------------------------
//
// Generated by www.easywsdl.com
// Version: 4.0.4.1
//
// Created by Quasar Development at 21-10-2014
//
//---------------------------------------------------


public class Functions
{
    public interface IFunc< Res>
    {
        Res Func() throws java.lang.Exception;
    }

    public interface IFunc1< T,Res>
    {
        Res Func(T p);
    }

    public interface IFunc2< T1,T2,Res>
    {
        Res Func(T1 p1,T2 p2);
    }

    public interface IAction
    {
        void Action();
    }

    public interface IAction1< T>
    {
        void Action(T p1);
    }
}
