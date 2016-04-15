/*
 * Copyright (c) 2002-2016 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.codegen.bytecode;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Modifier;

import org.neo4j.codegen.Expression;
import org.neo4j.codegen.ExpressionVisitor;
import org.neo4j.codegen.FieldReference;
import org.neo4j.codegen.LocalVariable;
import org.neo4j.codegen.MethodReference;
import org.neo4j.codegen.TypeReference;

import static org.neo4j.codegen.ByteCodeUtils.byteCodeName;
import static org.neo4j.codegen.ByteCodeUtils.desc;
import static org.neo4j.codegen.ByteCodeUtils.typeName;

class ByteCodeExpressionVisitor implements ExpressionVisitor, Opcodes
{
    private final MethodVisitor methodVisitor;

    ByteCodeExpressionVisitor( MethodVisitor methodVisitor )
    {
        this.methodVisitor = methodVisitor;
    }

    @Override
    public void invoke( Expression target, MethodReference method, Expression[] arguments )
    {
        target.accept( this );
        for ( Expression argument : arguments )
        {
            argument.accept( this );
        }
        if ( Modifier.isInterface( method.owner().modifiers() ) )
        {
            methodVisitor
                    .visitMethodInsn( INVOKEINTERFACE, byteCodeName( method.owner() ), method.name(),
                            desc( method ),
                            true );
        }
        else if ( method.isConstructor() )
        {
            methodVisitor
                    .visitMethodInsn( INVOKESPECIAL, byteCodeName( method.owner() ), method.name(), desc( method ),
                            false );
        }
        else
        {
            methodVisitor
                    .visitMethodInsn( INVOKEVIRTUAL, byteCodeName( method.owner() ), method.name(), desc( method ),
                            false );
        }
    }

    @Override
    public void invoke( MethodReference method, Expression[] arguments )
    {
        for ( Expression argument : arguments )
        {
            argument.accept( this );
        }
        methodVisitor.visitMethodInsn( INVOKESTATIC,
                byteCodeName( method.owner() ),
                method.name(), desc( method ), false );
    }

    @Override
    public void load( LocalVariable variable )
    {
        switch ( variable.type().simpleName() )
        {
        case "int":
        case "byte":
        case "short":
        case "char":
        case "boolean":
            methodVisitor.visitVarInsn( ILOAD, variable.index() );
            break;
        case "long":
            methodVisitor.visitVarInsn( LLOAD, variable.index() );
            break;
        case "float":
            methodVisitor.visitVarInsn( FLOAD, variable.index() );
            break;
        case "double":
            methodVisitor.visitVarInsn( DLOAD, variable.index() );
            break;
        default:
            methodVisitor.visitVarInsn( ALOAD, variable.index() );
        }
    }

    @Override
    public void getField( Expression target, FieldReference field )
    {
        target.accept( this );
        methodVisitor
                .visitFieldInsn( GETFIELD, byteCodeName( field.owner() ), field.name(), typeName( field.type() ) );
    }

    @Override
    public void constant( Object value )
    {
        if ( value == null )
        {
            methodVisitor.visitInsn( ACONST_NULL );
        }
        else if ( value instanceof Integer )
        {
            pushInteger( (Integer) value );
        }
        else if ( value instanceof Byte )
        {
            pushInteger( (Byte) value );
        }
        else if ( value instanceof Short )
        {
            pushInteger( (Short) value );
        }
        else if ( value instanceof Long )
        {
            methodVisitor.visitLdcInsn( value );
        }
        else if ( value instanceof Double )
        {
            methodVisitor.visitLdcInsn( value );
        }
        else if ( value instanceof Float )
        {
            methodVisitor.visitLdcInsn( value );
        }
        else if ( value instanceof Boolean )
        {
            boolean b = (boolean) value;
            methodVisitor.visitInsn( b ? ICONST_1 : ICONST_0 );
        }
        else
        {
            methodVisitor.visitLdcInsn( value );
        }
    }

    @Override
    public void getStatic( FieldReference field )
    {
        methodVisitor
                .visitFieldInsn( GETSTATIC, byteCodeName( field.owner() ), field.name(), typeName( field.type() ) );
    }

    @Override
    public void loadThis( String sourceName )
    {
        methodVisitor.visitVarInsn( ALOAD, 0 );
    }

    @Override
    public void newInstance( TypeReference type )
    {
        methodVisitor.visitTypeInsn( NEW, byteCodeName( type ) );
        methodVisitor.visitInsn( DUP );
    }

    @Override
    public void not( Expression expression )
    {
        expression.accept( this );
        Label l0 = new Label();
        methodVisitor.visitJumpInsn( IFNE, l0 );
        methodVisitor.visitInsn( ICONST_1 );
        Label l1 = new Label();
        methodVisitor.visitJumpInsn( GOTO, l1 );
        methodVisitor.visitLabel( l0 );
        methodVisitor.visitInsn( ICONST_0 );
        methodVisitor.visitLabel( l1 );
    }

    @Override
    public void ternary( Expression test, Expression onTrue, Expression onFalse )
    {
        test.accept( this );
        Label l0 = new Label();
        methodVisitor.visitJumpInsn( IFEQ, l0 );
        onTrue.accept( this );
        Label l1 = new Label();
        methodVisitor.visitJumpInsn( GOTO, l1 );
        methodVisitor.visitLabel( l0 );
        onFalse.accept( this );
        methodVisitor.visitLabel( l1 );
    }

    @Override
    public void eq( Expression lhs, Expression rhs, TypeReference type )
    {
        switch ( type.simpleName() )
        {
        case "int":
        case "byte":
        case "short":
        case "char":
        case "boolean":
            compareIntOrReferenceType( lhs, rhs, IF_ICMPNE );
            break;
        case "long":
            compareLongOrFloatType( lhs, rhs, LCMP, IFNE );
            break;
        case "float":
            compareLongOrFloatType( lhs, rhs, FCMPL, IFNE );
            break;
        case "double":
            compareLongOrFloatType( lhs, rhs, DCMPL, IFNE );
            break;
        default:
            compareIntOrReferenceType( lhs, rhs, IF_ACMPNE );
        }
    }

    @Override
    public void or( Expression lhs, Expression rhs )
    {
        /*
         * something like:
         *
         * LOAD lhs
         * IF TRUE GOTO 0
         * LOAD rhs
         * IF FALSE GOTO 1
         *
         * 0:
         *  LOAD TRUE
         *  GOTO 2
         * 1:
         *  LOAD FALSE
         * 2:
         *  ...continue doing stuff
         */
        lhs.accept( this );
        Label l0 = new Label();
        methodVisitor.visitJumpInsn( IFNE, l0 );
        rhs.accept( this );
        Label l1 = new Label();
        methodVisitor.visitJumpInsn( IFEQ, l1 );
        methodVisitor.visitLabel( l0 );
        methodVisitor.visitInsn( ICONST_1 );
        Label l2 = new Label();
        methodVisitor.visitJumpInsn( GOTO, l2 );
        methodVisitor.visitLabel( l1 );
        methodVisitor.visitInsn( ICONST_0 );
        methodVisitor.visitLabel( l2 );

    }

    @Override
    public void addInts( Expression lhs, Expression rhs )
    {
        lhs.accept( this );
        rhs.accept( this );
        methodVisitor.visitInsn( IADD );
    }

    @Override
    public void addLongs( Expression lhs, Expression rhs )
    {
        lhs.accept( this );
        rhs.accept( this );
        methodVisitor.visitInsn( LADD );
    }

    @Override
    public void addDoubles( Expression lhs, Expression rhs )
    {
        lhs.accept( this );
        rhs.accept( this );
        methodVisitor.visitInsn( DADD );
    }

    @Override
    public void gt( Expression lhs, Expression rhs, TypeReference type )
    {

        switch ( type.simpleName() )
        {
        case "int":
        case "byte":
        case "short":
        case "char":
        case "boolean":
            compareIntOrReferenceType( lhs, rhs, IF_ICMPLE );
            break;
        case "long":
            compareLongOrFloatType( lhs, rhs, LCMP, IFLE );
            break;
        case "float":
            compareLongOrFloatType( lhs, rhs, FCMPL, IFLE );
            break;
        case "double":
            compareLongOrFloatType( lhs, rhs, DCMPL, IFLE );
            break;
        default:
            throw new IllegalStateException( "Cannot compare reference types" );
        }
    }

    @Override
    public void subtractInts( Expression lhs, Expression rhs )
    {
        lhs.accept( this );
        rhs.accept( this );
        methodVisitor.visitInsn( ISUB );
    }

    @Override
    public void subtractLongs( Expression lhs, Expression rhs )
    {
        lhs.accept( this );
        rhs.accept( this );
        methodVisitor.visitInsn( LSUB );
    }

    @Override
    public void subtractDoubles( Expression lhs, Expression rhs )
    {
        lhs.accept( this );
        rhs.accept( this );
        methodVisitor.visitInsn( DSUB );
    }

    @Override
    public void multiplyDoubles( Expression lhs, Expression rhs )
    {
        lhs.accept( this );
        rhs.accept( this );
        methodVisitor.visitInsn( DMUL );
    }

    @Override
    public void multiplyLongs( Expression lhs, Expression rhs )
    {
        lhs.accept( this );
        rhs.accept( this );
        methodVisitor.visitInsn( LMUL );
    }

    @Override
    public void cast( TypeReference type, Expression expression )
    {
        expression.accept( this );
        methodVisitor.visitTypeInsn( CHECKCAST, byteCodeName( type ) );
    }

    @Override
    public void newArray( TypeReference type, Expression... exprs )
    {
        pushInteger( exprs.length );
        createArray( type );
        for ( int i = 0; i < exprs.length; i++ )
        {
            methodVisitor.visitInsn( DUP );
            pushInteger( i );
            exprs[i].accept( this );
            arrayStore( type );
        }
    }

    @Override
    public void longToDouble( Expression expression )
    {
        expression.accept( this );
        methodVisitor.visitInsn( L2D );
    }

    @Override
    public void pop( Expression expression )
    {
        expression.accept( this );
        methodVisitor.visitInsn( POP );
    }

    private void compareIntOrReferenceType( Expression lhs, Expression rhs, int opcode )
    {
        lhs.accept( this );
        rhs.accept( this );

        Label l0 = new Label();
        methodVisitor.visitJumpInsn( opcode, l0 );
        methodVisitor.visitInsn( ICONST_1 );
        Label l1 = new Label();
        methodVisitor.visitJumpInsn( GOTO, l1 );
        methodVisitor.visitLabel( l0 );
        methodVisitor.visitInsn( ICONST_0 );
        methodVisitor.visitLabel( l1 );
    }

    private void compareLongOrFloatType( Expression lhs, Expression rhs, int opcode, int compare )
    {
        lhs.accept( this );
        rhs.accept( this );

        methodVisitor.visitInsn( opcode );
        Label l0 = new Label();
        methodVisitor.visitJumpInsn( compare, l0 );
        methodVisitor.visitInsn( ICONST_1 );
        Label l1 = new Label();
        methodVisitor.visitJumpInsn( GOTO, l1 );
        methodVisitor.visitLabel( l0 );
        methodVisitor.visitInsn( ICONST_0 );
        methodVisitor.visitLabel( l1 );
    }

    private void pushInteger( int integer )
    {
        if ( integer < 6 && integer >= 0 )
        {
            methodVisitor.visitInsn( ICONST_0 + integer );
        }
        else if ( integer < Byte.MAX_VALUE && integer > Byte.MIN_VALUE )
        {
            methodVisitor.visitIntInsn( BIPUSH, integer );
        }
        else if ( integer < Short.MAX_VALUE && integer > Short.MIN_VALUE )
        {
            methodVisitor.visitIntInsn( SIPUSH, integer );
        }
        else
        {
            methodVisitor.visitLdcInsn( integer );
        }
    }

    private void createArray( TypeReference reference )
    {
        switch ( reference.name() )
        {
        case "int":
            methodVisitor.visitIntInsn( NEWARRAY, T_INT );
            break;
        case "long":
            methodVisitor.visitIntInsn( NEWARRAY, T_LONG );
            break;
        case "byte":
            methodVisitor.visitIntInsn( NEWARRAY, T_BYTE );
            break;
        case "short":
            methodVisitor.visitIntInsn( NEWARRAY, T_SHORT );
            break;
        case "char":
            methodVisitor.visitIntInsn( NEWARRAY, T_CHAR );
            break;
        case "float":
            methodVisitor.visitIntInsn( NEWARRAY, T_FLOAT );
            break;
        case "double":
            methodVisitor.visitIntInsn( NEWARRAY, T_DOUBLE );
            break;
        case "boolean":
            methodVisitor.visitIntInsn( NEWARRAY, T_BOOLEAN );
            break;
        default:
            methodVisitor.visitTypeInsn( ANEWARRAY, byteCodeName( reference ) );
        }
    }

    private void arrayStore( TypeReference reference )
    {
        switch ( reference.name() )
        {
        case "int":
            methodVisitor.visitInsn( IASTORE );
            break;
        case "long":
            methodVisitor.visitInsn( LASTORE );
            break;
        case "byte":
            methodVisitor.visitInsn( BASTORE );
            break;
        case "short":
            methodVisitor.visitInsn( SASTORE );
            break;
        case "char":
            methodVisitor.visitInsn( CASTORE );
            break;
        case "float":
            methodVisitor.visitInsn( FASTORE );
            break;
        case "double":
            methodVisitor.visitInsn( DASTORE );
            break;
        case "boolean":
            methodVisitor.visitInsn( BASTORE );
            break;
        default:
            methodVisitor.visitInsn( AASTORE );

        }
    }

    private int findAdder( TypeReference type )
    {
        switch ( type.simpleName() )
        {
        case "int":
        case "byte":
        case "short":
        case "char":
            return IADD;
        case "long":
            return LADD;
        case "float":
            return FADD;
        case "double":
            return DADD;
        default:
            throw new IllegalStateException( "Addition is only supported for primitive number types" );
        }
    }
}
