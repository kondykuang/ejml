/*
 * Copyright (c) 2009-2017, Peter Abeles. All Rights Reserved.
 *
 * This file is part of Efficient Java Matrix Library (EJML).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ejml.sparse.csc;

import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;
import org.ejml.sparse.csc.misc.ImplCommonOps_DSCC;
import org.ejml.sparse.csc.mult.ImplSparseSparseMult_DSCC;

import java.util.Arrays;

/**
 * @author Peter Abeles
 */
public class CommonOps_DSCC {

    /**
     * Checks to see if row indicies are sorted into ascending order.  O(N)
     * @return true if sorted and false if not
     */
    public static boolean checkIndicesSorted( DMatrixSparseCSC A ) {
        for (int j = 0; j < A.numCols; j++) {
            int idx0 = A.col_idx[j];
            int idx1 = A.col_idx[j+1];

            if( idx0 != idx1 && A.nz_rows[idx0] >= A.numRows )
                return false;

            for (int i = idx0+1; i < idx1; i++) {
                int row = A.nz_rows[i];
                if( A.nz_rows[i-1] >= row)
                    return false;
                if( row >= A.numRows )
                    return false;
            }
        }
        return true;
    }

    public static boolean checkSortedFlag( DMatrixSparseCSC A ) {
        if( A.indicesSorted )
            return checkIndicesSorted(A);
        return true;
    }

    /**
     * Perform matrix transpose
     *
     * @param a Input matrix.  Not modified
     * @param a_t Storage for transpose of 'a'.  Must be correct shape.  data length might be adjusted.
     * @param work Optional work matrix.  null or of length a.numRows
     */
    public static void transpose(DMatrixSparseCSC a , DMatrixSparseCSC a_t , int work[] ) {
        if( a_t.numRows != a.numCols || a_t.numCols != a.numRows )
            throw new IllegalArgumentException("Unexpected shape for transpose matrix");

        a_t.growMaxLength(a.nz_length, false);
        a_t.nz_length = a.nz_length;

        ImplCommonOps_DSCC.transpose(a, a_t, work);
    }

    public static void mult(DMatrixSparseCSC A , DMatrixSparseCSC B , DMatrixSparseCSC C ) {
        mult(A,B,C,null,null);
    }

    /**
     * Performs matrix multiplication.  C = A*B
     *
     * @param A Matrix
     * @param B Matrix
     * @param C Storage for results.  Data length is increased if increased if insufficient.
     * @param workA (Optional) Storage for internal work.  null or array of length A.numRows
     * @param workB (Optional) Storage for internal work.  null or array of length A.numRows
     */
    public static void mult(DMatrixSparseCSC A , DMatrixSparseCSC B , DMatrixSparseCSC C ,
                            int workA[], double workB[] )
    {
        if( A.numRows != C.numRows || B.numCols != C.numCols )
            throw new IllegalArgumentException("Inconsistent matrix shapes");

        ImplSparseSparseMult_DSCC.mult(A,B,C, workA, workB);
    }

    /**
     * Performs matrix multiplication.  C = A*B
     *
     * @param A Matrix
     * @param B Dense Matrix
     * @param C Dense Matrix
     */
    public static void mult(DMatrixSparseCSC A , DMatrixRMaj B , DMatrixRMaj C )
    {
        if( A.numRows != C.numRows || B.numCols != C.numCols )
            throw new IllegalArgumentException("Inconsistent matrix shapes");

        ImplSparseSparseMult_DSCC.mult(A,B,C);
    }

    /**
     * Performs matrix addition:<br>
     * C = &alpha;A + &beta;B
     *
     * @param alpha scalar value multiplied against A
     * @param A Matrix
     * @param beta scalar value multiplied against B
     * @param B Matrix
     * @param C Output matrix.
     * @param work0 (Optional) Work space of length A.rows.  Null to declare internally
     * @param work1 (Optional) Work space of length A.rows.  Null to declare internally
     */
    public static void add(double alpha, DMatrixSparseCSC A, double beta, DMatrixSparseCSC B, DMatrixSparseCSC C,
                           int work0[], double work1[])
    {
        if( A.numRows != B.numRows || A.numCols != B.numCols || A.numRows != C.numRows || A.numCols != C.numCols)
            throw new IllegalArgumentException("Inconsistent matrix shapes");

        ImplCommonOps_DSCC.add(alpha,A,beta,B,C, work0, work1);
    }

    public static DMatrixSparseCSC identity(int length ) {
        return identity(length, length);
    }

    public static DMatrixSparseCSC identity(int numRows , int numCols ) {
        int min = Math.min(numRows, numCols);
        DMatrixSparseCSC A = new DMatrixSparseCSC(numRows, numCols, min);

        Arrays.fill(A.nz_values,0,min,1);
        for (int i = 1; i <= min; i++) {
            A.col_idx[i] = i;
            A.nz_rows[i-1] = i-1;
        }
        for (int i = min+1; i <= numCols; i++) {
            A.col_idx[i] = min;
        }

        return A;
    }

    public static void scale(double scalar, DMatrixSparseCSC A, DMatrixSparseCSC B) {
        if( A.numRows != B.numRows || A.numCols != B.numCols )
            throw new IllegalArgumentException("Unexpected shape for transpose matrix");
        B.copyStructure(A);

        for(int i = 0; i < A.nz_length; i++ ) {
            B.nz_values[i] = A.nz_values[i]*scalar;
        }
    }

    public static void divide(DMatrixSparseCSC A , double scalar , DMatrixSparseCSC B ) {
        if( A.numRows != B.numRows || A.numCols != B.numCols )
            throw new IllegalArgumentException("Unexpected shape for transpose matrix");
        B.copyStructure(A);

        for(int i = 0; i < A.nz_length; i++ ) {
            B.nz_values[i] = A.nz_values[i]/scalar;
        }
    }

    public static double elementMinAbs( DMatrixSparseCSC A ) {
        if( A.nz_length == 0)
            return 0;

        double min = A.isFull() ? Math.abs(A.nz_values[0]) : 0;
        for(int i = 0; i < A.nz_length; i++ ) {
            double val = Math.abs(A.nz_values[i]);
            if( val < min ) {
                min = val;
            }
        }

        return min;
    }

    public static double elementMaxAbs( DMatrixSparseCSC A ) {
        if( A.nz_length == 0)
            return 0;

        double max = A.isFull() ? Math.abs(A.nz_values[0]) : 0;
        for(int i = 0; i < A.nz_length; i++ ) {
            double val = Math.abs(A.nz_values[i]);
            if( val > max ) {
                max = val;
            }
        }

        return max;
    }

    public static double elementMin( DMatrixSparseCSC A ) {
        if( A.nz_length == 0)
            return 0;

        double min = A.isFull() ? A.nz_values[0] : 0;
        for(int i = 0; i < A.nz_length; i++ ) {
            double val = A.nz_values[i];
            if( val < min ) {
                min = val;
            }
        }

        return min;
    }

    public static double elementMax( DMatrixSparseCSC A ) {
        if( A.nz_length == 0)
            return 0;

        double max = A.isFull() ? A.nz_values[0] : 0;
        for(int i = 0; i < A.nz_length; i++ ) {
            double val = A.nz_values[i];
            if( val > max ) {
                max = val;
            }
        }

        return max;
    }

    public static DMatrixSparseCSC diag(double... values ) {
        int N = values.length;
        DMatrixSparseCSC A = new DMatrixSparseCSC(N,N,N);

        for (int i = 0; i < N; i++) {
            A.col_idx[i+1] = i+1;
            A.nz_rows[i] = i;
            A.nz_values[i] = values[i];
        }

        return A;
    }

    /**
     * Converts the permutation vector into a matrix. B = P*A.  B[p[i],:] = A[i,:]
     *
     * @param p (Input) Permutation vector
     * @param P (Output) Permutation matrix
     */
    public static DMatrixSparseCSC permutationMatrix( int p[] , DMatrixSparseCSC P) {

        int N = p.length;

        if( P == null )
            P = new DMatrixSparseCSC(N,N,N);
        else
            P.reshape(N,N,N);
        P.indicesSorted = true;

        // each column should have one element inside of it
        for (int i = 0; i < N; i++) {
            P.col_idx[i+1] = i+1;
            P.nz_rows[p[i]] = i;
            P.nz_values[i] = 1;
        }

        return P;
    }

    /**
     * Converts the permutation matrix into a vector
     * @param P (Input) Permutation matrix
     * @param vector (Output) Permutation vector
     */
    public static void permutationVector( DMatrixSparseCSC P , int[] vector) {
        if( P.numCols != P.numRows ) {
            throw new IllegalArgumentException("Expected a square matrix");
        } else if( P.nz_length != P.numCols ) {
            throw new IllegalArgumentException("Expected N non-zero elements in permutation matrix");
        } else if( vector.length < P.numCols ) {
            throw new IllegalArgumentException("vector is too short");
        }

        int M = P.numCols;

        for (int i = 0; i < M; i++) {
            if( P.col_idx[i+1] != i+1 )
                throw new IllegalArgumentException("Unexpected number of elements in a column");

            vector[P.nz_rows[i]] = i;
        }
    }

    /**
     * Computes the inverse permutation vector
     * @param original Original permutation vector
     * @param inverse It's inverse
     */
    public static void permutationInverse( int []original , int []inverse ) {
        for (int i = 0; i < original.length; i++) {
            inverse[original[i]] = i;
        }
    }

    /**
     * Applies the row permutation specified by the vector to the input matrix and save the results
     * in the output matrix.  output[perm[j],:] = input[j,:]
     *  @param permInv (Input) Inverse permutation vector.  Specifies new order of the rows.
     * @param input (Input) Matrix which is to be permuted
     * @param output (Output) Matrix which has the permutation stored in it.  Is reshaped.
     */
    public static void permuteRowInv(int permInv[], DMatrixSparseCSC input, DMatrixSparseCSC output) {
        if( input.numRows != permInv.length )
            throw new IllegalArgumentException("Number of rows in input must match length of permutation vector");

        output.reshape(input.numRows,input.numCols,input.nz_length);
        output.indicesSorted = false;

        System.arraycopy(input.nz_values,0,output.nz_values,0,input.nz_length);
        System.arraycopy(input.col_idx,0,output.col_idx,0,input.numCols+1);

        int M = permInv.length;
        int idx0 = 0;
        for (int i = 0; i < M; i++) {
            int idx1 = output.col_idx[i+1];

            for (int j = idx0; j < idx1; j++) {
                output.nz_rows[j] = permInv[input.nz_rows[j]];
            }
            idx0 = idx1;
        }
    }

    /**
     * Applies the forward column and inverse row permutation specified by the two vector to the input matrix
     * and save the results in the output matrix. output[permRow[j],permCol[i]] = input[j,i]
     * @param permRowInv (Input) Inverse row permutation vector
     * @param input (Input) Matrix which is to be permuted
     * @param permCol (Input) Column permutation vector
     * @param output (Output) Matrix which has the permutation stored in it.  Is reshaped.
     */
    public static void permute(int permRowInv[], DMatrixSparseCSC input, int permCol[], DMatrixSparseCSC output) {
        if( input.numRows != permRowInv.length )
            throw new IllegalArgumentException("Number of column in input must match length of rowInv");
        if( input.numCols != permCol.length )
            throw new IllegalArgumentException("Number of rows in input must match length of colInv");

        output.reshape(input.numRows,input.numCols,input.nz_length);
        output.indicesSorted = false;
        output.col_idx[0] = 0;

        int N = input.numCols;

        // traverse through in order for the output columns
        int outputNZ = 0;
        for (int i = 0; i < N; i++) {
            int inputCol = permCol[i]; // column of input to source from
            int inputNZ = input.col_idx[inputCol];
            int total = input.col_idx[inputCol+1]- inputNZ; // total nz in this column

            output.col_idx[i+1] = output.col_idx[i] + total;

            for (int j = 0; j < total; j++) {
                output.nz_rows[outputNZ] = permRowInv[input.nz_rows[inputNZ]];
                output.nz_values[outputNZ++] = input.nz_values[inputNZ++];
            }
        }
    }

}
