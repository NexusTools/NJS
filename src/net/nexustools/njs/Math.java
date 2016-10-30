/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.nexustools.njs;

import org.apache.commons.math3.util.FastMath;

/**
 *
 * @author kate
 */
public class Math extends GenericObject {
	
	public Math(final Global global) {
		super(global);
		
		final String.Instance _toSource = global.wrap("Math");
		
		setHidden("abs", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(params.length == 0)
					return global.NaN;
				return global.wrap(FastMath.abs(global.toNumber(params[0]).number));
			}
			@Override
			public java.lang.String name() {
				return "abs";
			}
		});
		setHidden("acos", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(params.length == 0)
					return global.NaN;
				return global.wrap(FastMath.acos(global.toNumber(params[0]).number));
			}
			@Override
			public java.lang.String name() {
				return "acos";
			}
		});
		setHidden("acosh", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(params.length == 0)
					return global.NaN;
				double a = global.toNumber(params[0]).number;
				return global.wrap(FastMath.log(a + java.lang.Math.sqrt(a * a - 1)));
			}
			@Override
			public java.lang.String name() {
				return "acosh";
			}
		});
		setHidden("asin", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(params.length == 0)
					return global.NaN;
				return global.wrap(FastMath.asin(global.toNumber(params[0]).number));
			}
			@Override
			public java.lang.String name() {
				return "asin";
			}
		});
		setHidden("asinh", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(params.length == 0)
					return global.NaN;
				return global.wrap(FastMath.asinh(global.toNumber(params[0]).number));
			}
			@Override
			public java.lang.String name() {
				return "asinh";
			}
		});
		setHidden("atan", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(params.length == 0)
					return global.NaN;
				return global.wrap(FastMath.atan(global.toNumber(params[0]).number));
			}
			@Override
			public java.lang.String name() {
				return "atan";
			}
		});
		setHidden("atanh", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(params.length == 0)
					return global.NaN;
				return global.wrap(FastMath.atanh(global.toNumber(params[0]).number));
			}
			@Override
			public java.lang.String name() {
				return "atanh";
			}
		});
		setHidden("atan2", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(params.length < 2)
					return global.NaN;
				return global.wrap(FastMath.atan2(global.toNumber(params[0]).number, global.toNumber(params[1]).number));
			}
			@Override
			public java.lang.String name() {
				return "atan2";
			}
		});
		setHidden("cbrt", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(params.length == 0)
					return global.NaN;
				return global.wrap(FastMath.cbrt(global.toNumber(params[0]).number));
			}
			@Override
			public java.lang.String name() {
				return "cbrt";
			}
		});
		setHidden("ceil", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(params.length == 0)
					return global.NaN;
				return global.wrap(FastMath.ceil(global.toNumber(params[0]).number));
			}
			@Override
			public java.lang.String name() {
				return "ceil";
			}
		});
		/*setHidden("clz32", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(params.length == 0)
					return global.NaN;
				return global.wrap(FastMath.clz32(global.toNumber(params[0]).number));
			}
			@Override
			public java.lang.String name() {
				return "clz32";
			}
		});*/
		setHidden("cos", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(params.length == 0)
					return global.NaN;
				return global.wrap(FastMath.cos(global.toNumber(params[0]).number));
			}
			@Override
			public java.lang.String name() {
				return "cos";
			}
		});
		setHidden("cosh", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(params.length == 0)
					return global.NaN;
				return global.wrap(FastMath.cosh(global.toNumber(params[0]).number));
			}
			@Override
			public java.lang.String name() {
				return "cosh";
			}
		});
		setHidden("exp", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(params.length == 0)
					return global.NaN;
				return global.wrap(FastMath.exp(global.toNumber(params[0]).number));
			}
			@Override
			public java.lang.String name() {
				return "exp";
			}
		});
		setHidden("expm1", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(params.length == 0)
					return global.NaN;
				return global.wrap(FastMath.expm1(global.toNumber(params[0]).number));
			}
			@Override
			public java.lang.String name() {
				return "expm1";
			}
		});
		setHidden("floor", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(params.length == 0)
					return global.NaN;
				return global.wrap(FastMath.floor(global.toNumber(params[0]).number));
			}
			@Override
			public java.lang.String name() {
				return "floor";
			}
		});
		/*setHidden("fround", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(params.length == 0)
					return global.NaN;
				return global.wrap(FastMath.fround(global.toNumber(params[0]).number));
			}
			@Override
			public java.lang.String name() {
				return "fround";
			}
		});*/
		/*setHidden("hypot", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(params.length < 2)
					return global.NaN;
				return global.wrap(FastMath.hypot(global.toNumber(params[0]).number, global.toNumber(params[1]).number));
			}
			@Override
			public java.lang.String name() {
				return "hypot";
			}
		});*/
		/*setHidden("imul", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(params.length == 0)
					return global.NaN;
				return global.wrap(FastMath.imul(global.toNumber(params[0]).number));
			}
			@Override
			public java.lang.String name() {
				return "imul";
			}
		});*/
		setHidden("log", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(params.length == 0)
					return global.NaN;
				return global.wrap(FastMath.log(global.toNumber(params[0]).number));
			}
			@Override
			public java.lang.String name() {
				return "log";
			}
		});
		setHidden("log1p", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(params.length == 0)
					return global.NaN;
				return global.wrap(FastMath.log1p(global.toNumber(params[0]).number));
			}
			@Override
			public java.lang.String name() {
				return "log1p";
			}
		});
		setHidden("log10", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(params.length == 0)
					return global.NaN;
				return global.wrap(FastMath.log10(global.toNumber(params[0]).number));
			}
			@Override
			public java.lang.String name() {
				return "log10";
			}
		});
		/*setHidden("log2", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(params.length == 0)
					return global.NaN;
				return global.wrap(FastMath.log2(global.toNumber(params[0]).number));
			}
			@Override
			public java.lang.String name() {
				return "log2";
			}
		});*/
		setHidden("max", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(params.length == 0)
					return global.NaN;
				double max = Double.NEGATIVE_INFINITY;
				for(BaseObject param : params) {
					double value = global.toNumber(param).number;
					if(value > max)
						max = value;
				}
				return global.wrap(max);
			}
			@Override
			public java.lang.String name() {
				return "max";
			}
		});
		setHidden("min", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(params.length == 0)
					return global.NaN;
				double max = Double.POSITIVE_INFINITY;
				for(BaseObject param : params) {
					double value = global.toNumber(param).number;
					if(value < max)
						max = value;
				}
				return global.wrap(max);
			}
			@Override
			public java.lang.String name() {
				return "min";
			}
		});
		setHidden("pow", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(params.length < 2)
					return global.NaN;
				return global.wrap(FastMath.pow(global.toNumber(params[0]).number, global.toNumber(params[1]).number));
			}
			@Override
			public java.lang.String name() {
				return "pow";
			}
		});
		setHidden("round", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(params.length == 0)
					return global.NaN;
				return global.wrap(FastMath.round(global.toNumber(params[0]).number));
			}
			@Override
			public java.lang.String name() {
				return "round";
			}
		});
		setHidden("sign", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(params.length == 0)
					return global.NaN;
				double number = global.toNumber(params[0]).number;
				if(Double.isNaN(number))
					return global.NaN;
				if(number == 0)
					return global.Zero;
				if(number < 0)
					return global.NegativeOne;
				return global.PositiveOne;
			}
			@Override
			public java.lang.String name() {
				return "sign";
			}
		});
		setHidden("sin", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(params.length == 0)
					return global.NaN;
				return global.wrap(FastMath.sin(global.toNumber(params[0]).number));
			}
			@Override
			public java.lang.String name() {
				return "sin";
			}
		});
		setHidden("sinh", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(params.length == 0)
					return global.NaN;
				return global.wrap(FastMath.sinh(global.toNumber(params[0]).number));
			}
			@Override
			public java.lang.String name() {
				return "sinh";
			}
		});
		setHidden("sqrt", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(params.length == 0)
					return global.NaN;
				return global.wrap(FastMath.sqrt(global.toNumber(params[0]).number));
			}
			@Override
			public java.lang.String name() {
				return "sqrt";
			}
		});
		setHidden("tan", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(params.length == 0)
					return global.NaN;
				return global.wrap(FastMath.tan(global.toNumber(params[0]).number));
			}
			@Override
			public java.lang.String name() {
				return "tan";
			}
		});
		setHidden("tanh", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(params.length == 0)
					return global.NaN;
				return global.wrap(FastMath.tanh(global.toNumber(params[0]).number));
			}
			@Override
			public java.lang.String name() {
				return "tanh";
			}
		});
		setHidden("toSource", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				return _toSource;
			}
			@Override
			public java.lang.String name() {
				return "toSource";
			}
		});
		/*setHidden("trunc", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				if(params.length == 0)
					return global.NaN;
				return global.wrap(FastMath.trunc(global.toNumber(params[0]).number));
			}
			@Override
			public java.lang.String name() {
				return "trunc";
			}
		});*/
		setHidden("random", new AbstractFunction(global) {
			@Override
			public BaseObject call(BaseObject _this, BaseObject... params) {
				return global.wrap(java.lang.Math.random());
			}
			@Override
			public java.lang.String name() {
				return "random"; //To change body of generated methods, choose Tools | Templates.
			}
		});
		setHidden("E", global.wrap(java.lang.Math.E));
		setHidden("LN2", global.wrap(0.6931471805599453));
		setHidden("LN10", global.wrap(2.302585092994046));
		setHidden("LOG2E", global.wrap(1.4426950408889634));
		setHidden("LOG10E", global.wrap(0.4342944819032518));
		setHidden("PI", global.wrap(java.lang.Math.PI));
		setHidden("SQRT1_2", global.wrap(0.7071067811865476));
		setHidden("SQRT2", global.wrap(1.4142135623730951));
	}
	
}
