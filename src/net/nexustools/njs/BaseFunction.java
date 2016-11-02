/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.nexustools.njs;

/**
 *
 * @author kate
 */
public interface BaseFunction extends BaseObject {
	public java.lang.String name();
	public java.lang.String source();
	public java.lang.String arguments();
	public BaseObject call(BaseObject _this, BaseObject... params);
	public BaseObject construct(BaseObject... params);
	public void setPrototype(BaseObject prototype);
	public BaseObject prototype();
	public BaseObject create();
}
